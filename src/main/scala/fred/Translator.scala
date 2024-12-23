package fred

import scala.collection.mutable

import fred.Compiler.RcAlgo
import fred.Compiler.Settings

object Translator {
  private val This = "this"

  private val NoMangleFns = Set("main", "printf")

  /** @param file
    * @param rcAlgo
    *   The cyclic reference counting algorithm to use. With
    *   [[RcAlgo.LazyMarkScan]], everything gets put into the same SCC
    * @param typer
    * @return
    */
  def toC(file: ParsedFile, settings: Settings = Settings())(using
      typer: Typer
  ): String = {
    given Bindings = Bindings.fromFile(file)
    given cycles: Cycles = settings.rcAlgo match {
      case RcAlgo.LazyMarkScan => Cycles(
          List(file.typeDefs.toSet),
          file.typeDefs.map(_ -> 0).toMap,
          // TODO it's possible that there aren't any cycles possible in the
          // whole program, make this an empty set in that case
          Set(0)
        )
      case RcAlgo.Mine => Cycles.fromFile(file)
    }
    val helper = Helper(typer, cycles)
    val (genDecls, genImpls) =
      List(Freer, Decrementer, MarkGray, Scan, ScanBlack, CollectWhite, Printer)
        .flatMap(gen => file.typeDefs.map(td => (gen.decl(td), gen.impl(td))))
        .unzip
    val (fnDecls, fnImpls) = file.fns.map(helper.fnToC).unzip
    val ctorDecls = file.typeDefs
      .flatMap(typ => typ.cases.map(variant => s"${ctorSig(typ, variant)};"))
    val ctorImpls = file.typeDefs.flatMap { typ =>
      typ.cases.map { variant =>
        s"${ctorSig(typ, variant)} {\n${indent(1)(ctorImpl(typ, variant))}\n}"
      }
    }
    val generated = file.typeDefs.map(helper.translateType)
      .mkString("", "\n", "\n") + genDecls.mkString("", "\n", "\n") +
      ctorDecls.mkString("", "\n", "\n") + fnDecls.mkString("", "\n", "\n") +
      genImpls.mkString("", "\n", "\n") + ctorImpls.mkString("", "\n", "\n") +
      fnImpls.mkString("", "\n", "\n")
    val extraIncludes =
      if (settings.includeMemcheck) "#include \"memcheck.h\"\n" else ""
    extraIncludes + "#include \"runtime.h\"\n\n" +
      generated.replaceAll(raw"\n(\s|\n)*\n", "\n").strip() + "\n"
  }

  private trait GeneratedFn(unmangledName: String) {
    def returnType: String

    def body(typ: TypeDef)(using Bindings, Cycles): String

    def name(typ: TypeDef) = s"$$${unmangledName}_${typ.name}"

    private def sig(typ: TypeDef) =
      s"$returnType ${name(typ)}(${typeRefToC(typ.name)} $This)"

    def decl(typ: TypeDef) = s"${sig(typ)};"

    def impl(typ: TypeDef)(using Bindings, Cycles) =
      s"${sig(typ)} {\n${indent(1)(body(typ))}\n}"
  }

  /** Free the object and decrement the refcounts of any objects it refers to
    * that are NOT in the same SCC
    */
  private object Freer extends GeneratedFn("free") {
    override def returnType = "void"

    override def body(
        typ: TypeDef
    )(using bindings: Bindings, cycles: Cycles): String = {
      val myScc = cycles.sccMap(typ)

      val cases = switch(This, typ) {
        case variant @ EnumCase(Spanned(ctorName, _), fields, _) => fields
            .map { case FieldDef(_, fieldName, fieldTypeRef, _) =>
              bindings.types(fieldTypeRef.name) match {
                case fieldType: TypeDef if cycles.sccMap(fieldType) != myScc =>
                  val mangled = cFieldName(fieldName.value, typ, variant)
                  s"${decrRc(s"$This->$mangled", fieldType)}"
                case _ => ""
              }
            }.mkString("\n")
      }

      raw"""|$cases
            |free($This);""".stripMargin
    }
  }

  /** Generate the signature and implementation for the decrementer function */
  private object Decrementer extends GeneratedFn("decr") {
    override def returnType = "void"

    override def body(
        typ: TypeDef
    )(using bindings: Bindings, cycles: Cycles): String = {
      // Cases for deleting the object
      val deleteCases = indent(1) {
        switch(This, typ) {
          case variant @ EnumCase(Spanned(ctorName, _), fields, _) => fields
              .map { case FieldDef(_, fieldName, fieldType, _) =>
                val mangled = cFieldName(fieldName.value, typ, variant)
                s"${decrRc(s"$This->$mangled", bindings.types(fieldType.name))}"
              }.mkString("\n")
        }
      }
      val scc = cycles.sccMap(typ)
      // No need to add a PCR if this type isn't in a bad SCC
      val addPCR = raw"""| else {
                         |  addPCR(
                         |    (void *) $This,
                         |    $scc,
                         |    (void *) ${MarkGray.name(typ)},
                         |    (void *) ${Scan.name(typ)},
                         |    (void *) ${CollectWhite.name(typ)});
                         |}""".stripMargin

      raw"""|if (--$This->rc == 0) {
            |$deleteCases
            |  removePCR((void *) $This, $scc);
            |  free($This);
            |}${if (cycles.badSCCs.contains(scc)) addPCR else ""}""".stripMargin
    }
  }

  private object MarkGray extends GeneratedFn("markGray") {
    override def returnType: String = "void"

    override def body(
        typ: TypeDef
    )(using bindings: Bindings, cycles: Cycles): String = {
      val myScc = cycles.sccMap(typ)
      val recMarks = switch(This, typ) { variant =>
        variant.fields.map { case FieldDef(_, fieldName, fieldTypeRef, _) =>
          val mangled = cFieldName(fieldName.value, typ, variant)
          bindings.types(fieldTypeRef.name) match {
            case fieldType: TypeDef =>
              if (cycles.sccMap(fieldType) == myScc) {
                s"""|$This->$mangled->rc --;
                    |${MarkGray.name(fieldType)}($This->$mangled);""".stripMargin
              } else { "" }
            case _ => ""
          }
        }.mkString("\n")
      }

      s"""|if ($This->color == kGray) return;
          |$This->color = kGray;
          |$This->addedPCR = 0;
          |$recMarks""".stripMargin
    }
  }

  private object Scan extends GeneratedFn("scan") {
    override def returnType: String = "void"

    override def body(
        typ: TypeDef
    )(using bindings: Bindings, cycles: Cycles): String = {
      val myScc = cycles.sccMap(typ)
      val recScan = switch(This, typ) { variant =>
        variant.fields.map { case FieldDef(_, fieldName, fieldTypeRef, _) =>
          val mangled = cFieldName(fieldName.value, typ, variant)
          bindings.types(fieldTypeRef.name) match {
            case fieldType: TypeDef =>
              if (cycles.sccMap(fieldType) == myScc)
                s"${Scan.name(fieldType)}($This->$mangled);"
              else ""
            case _ => ""
          }
        }.mkString("\n")
      }

      s"""|if ($This->color != kGray) return;
          |if ($This->rc > 0) {
          |  ${ScanBlack.name(typ)}($This);
          |  return;
          |}
          |$This->color = kWhite;
          |$recScan""".stripMargin
    }
  }

  private object ScanBlack extends GeneratedFn("scanBlack") {
    override def returnType: String = "void"

    override def body(
        typ: TypeDef
    )(using bindings: Bindings, cycles: Cycles): String = {
      val myScc = cycles.sccMap(typ)
      val recScan = indent(1) {
        switch(This, typ) { variant =>
          variant.fields.map { case FieldDef(_, fieldName, fieldTypeRef, _) =>
            val mangled = cFieldName(fieldName.value, typ, variant)
            bindings.types(fieldTypeRef.name) match {
              case fieldType: TypeDef =>
                if (cycles.sccMap(fieldType) == myScc) {
                  s"""|$This->$mangled->rc ++;
                      |${ScanBlack.name(fieldType)}($This->$mangled);"""
                    .stripMargin
                } else { "" }
              case _ => ""
            }
          }.mkString("\n")
        }
      }
      s"""|if ($This->color != kBlack) {
          |  $This->color = kBlack;
          |$recScan
          |}""".stripMargin
    }
  }

  private object CollectWhite extends GeneratedFn("collectWhite") {
    override def returnType: String = "void"

    override def body(
        typ: TypeDef
    )(using bindings: Bindings, cycles: Cycles): String = {
      val rec = indent(1) {
        switch(This, typ) { variant =>
          variant.fields.map { case FieldDef(_, fieldName, fieldTypeRef, _) =>
            val mangled = cFieldName(fieldName.value, typ, variant)
            bindings.types(fieldTypeRef.name) match {
              case fieldType: TypeDef =>
                s"${CollectWhite.name(fieldType)}($This->$mangled);"
              case _ => ""
            }
          }.mkString("\n")
        }
      }
      raw"""|if ($This->color == kWhite) {
            |  $This->color = kBlack;
            |$rec
            |  struct FreeCell *curr = freeList;
            |  freeList = malloc(sizeof(struct FreeCell));
            |  freeList->obj = (void *) $This;
            |  freeList->next = curr;
            |  freeList->free = (void *) ${Freer.name(typ)};
            |}""".stripMargin
    }
  }

  /** Print the object (no newline at end) */
  private object Printer extends GeneratedFn("print") {
    override def returnType = "void"

    override def body(
        typ: TypeDef
    )(using bindings: Bindings, cycles: Cycles): String = {
      switch(This, typ) {
        case variant @ EnumCase(Spanned(ctorName, _), fields, _) =>
          val printFields = fields
            .map { case FieldDef(_, fieldName, fieldType, _) =>
              val mangled = cFieldName(fieldName.value, typ, variant)
              raw"""|printf("${fieldName.value}=");
                    |${callPrint(s"$This->$mangled", bindings.types(fieldType.name))}
                    |printf(", ");""".stripMargin
            }.mkString("\n")

          s"""|printf("$ctorName {");
              |$printFields
              |printf("}");""".stripMargin
      }
    }
  }

  private def ctorName(variant: EnumCase): String =
    s"new$$${variant.name.value}"

  private def ctorSig(typ: TypeDef, variant: EnumCase): String = {
    val params = variant.fields.toSeq.sortBy(_.name.value)
      .map(field => s"${typeRefToC(field.typ.name)} ${field.name.value}")
      .mkString(", ")
    s"${typeRefToC(typ.name)} ${ctorName(variant)}($params)"
  }

  private def ctorImpl(typ: TypeDef, variant: EnumCase)(using
      bindings: Bindings
  ): String = {
    val resVar = "$res"
    val setFields = variant.fields.map { field =>
      val fieldAccess =
        s"$resVar->${cFieldName(field.name.value, typ, variant)}"
      s"""|$fieldAccess = ${field.name.value};
          |${incrRc(fieldAccess, bindings.getType(field.typ))}""".stripMargin
    }.mkString("\n")
    s"""|${typeRefToC(typ.name)} $resVar = malloc(sizeof (struct ${typ.name}));
        |$resVar->rc = 0;
        |$resVar->color = kBlack;
        |$resVar->addedPCR = 0;
        |$resVar->print = ${Printer.name(typ)};
        |$resVar->kind = ${tagName(variant.name.value)};
        |$setFields
        |return $resVar;""".stripMargin
  }

  private def switch(expr: String, typ: TypeDef)(
      createArm: EnumCase => String
  ): String = {
    val armsToC = typ.cases.map { variant =>
      s"""|case ${tagName(variant.name.value)}:
          |${indent(1)(createArm(variant))}
          |  break;""".stripMargin
    }.mkString("\n")
    s"""|switch ($expr->kind) {
        |$armsToC
        |}""".stripMargin
  }

  private def typeRefToC(typeName: String) = {
    typeName match {
      case "int" => "int"
      case "str" => "char*"
      case name  => s"struct $name*"
    }
  }

  /** Apply `2 * level` spaces of indentation to every line in the given string
    */
  private def indent(level: Int)(s: String): String = {
    s.split("\n").map(line => "  " * level + line).mkString("\n")
  }

  private def callPrint(expr: String, typ: Type): String = {
    typ match {
      case td: TypeDef     => s"${Printer.name(td)}($expr);"
      case BuiltinType.Str => s"printf(\"%s\", $expr);"
      case BuiltinType.Int => s"printf(\"%d\", $expr);"
    }
  }

  private def incrRc(expr: String, typ: Type) = {
    if (typ.isInstanceOf[TypeDef]) { s"$expr->rc ++;" }
    else { "" }
  }

  private def decrRc(expr: String, typ: Type) = {
    typ match {
      case td: TypeDef => s"${Decrementer.name(td)}($expr);"
      case _           => ""
    }
  }

  /** Get the name of this field in C (mangled if necessary) */
  private def cFieldName(fieldName: String, typ: TypeDef, variant: EnumCase) = {
    if (typ.hasCommonField(fieldName)) fieldName
    else mangledFieldName(variant, fieldName)
  }

  private def mangleFnName(fnName: String) =
    if (NoMangleFns.contains(fnName)) fnName else "fn$" + fnName

  /** Field names need to be mangled so that multiple cases can have fields with
    * the same name
    *
    * @param enumCase
    *   The enum case/variant inside which this field is
    */
  private def mangledFieldName(enumCase: EnumCase, field: String) = {
    s"${field}_${enumCase.name.value}"
  }

  /** Mangle a constructor name to use it as the tag for a tagged union
    * representing the original ADT
    */
  private def tagName(ctorName: String): String = s"${ctorName}_tag"

  private class Helper(typer: Typer, cycles: Cycles) {

    /** Contains a mapping of mangled field names for every type */
    val mangledFieldsFor = mutable.Map.empty[TypeDef, Map[String, String]]

    val mangledVars = mutable.Map.empty[VarDef, String]

    val lastUsagesPost = mutable.Map.empty[Expr, Set[VarDef]]
    val lastUsagesPre = mutable.Map.empty[Expr, Set[VarDef]]

    val unusedVars = mutable.Set.empty[VarDef]

    private var resVarCounter = 0

    def translateType(typ: TypeDef): String = {
      val name = typ.name
      val tagNames = typ.cases.map(enumCase => tagName(enumCase.name.value))
        .mkString(", ")
      val tagEnum = s"enum ${name}_kind { ${tagNames} };"

      val mangledFields = mutable.Map.empty[String, String]

      val commonFields = typ.cases.head.fields.filter { field =>
        typ.hasCommonField(field.name.value)
      }
      val commonFieldsToC = commonFields.map { field =>
        s"  ${typeRefToC(field.typ.name)} ${field.name.value};"
      }.mkString("\n")
      mangledFields ++=
        commonFields.map(field => field.name.value -> field.name.value)

      val cases = typ.cases.map { enumCase =>
        val variantFields = enumCase.fields.filter { field =>
          commonFields.forall(_.name.value != field.name.value)
        }

        mangledFields ++= variantFields.map(field =>
          field.name.value -> mangledFieldName(enumCase, field.name.value)
        )

        val fields = variantFields.map { field =>
          s"${typeRefToC(field.typ.name)} ${mangledFieldName(enumCase, field.name.value)};"
        }.mkString(" ")
        s"struct { $fields };"
      }.mkString("\n    ")

      mangledFieldsFor.put(typ, mangledFields.toMap)

      val struct = s"""|struct $name {
                       |  int rc;
                       |  enum Color color;
                       |  int addedPCR;
                       |  enum ${name}_kind kind;
                       |  void (*print)();
                       |$commonFieldsToC
                       |  union {
                       |    $cases
                       |  };
                       |};""".stripMargin
      tagEnum + "\n" + struct
    }

    /** Returns code for the function's declaration and implementation */
    def fnToC(fn: FnDef)(using bindings: Bindings): (String, String) = {
      given Bindings = bindings.enterFn(fn)
      lastUsagesPre.clear()
      lastUsagesPost.clear()
      unusedVars.clear()

      val paramDefs = fn.params
        .map(param => VarDef.Param(param, bindings.getType(param.typ))).toSet
      val unusedParams = findLastUsages(fn.body, paramDefs).map(_.name)
      assert(
        unusedParams.subsetOf(paramDefs.map(_.name)),
        s"Function: ${fn.name.value}, unused vars: $unusedParams"
      )

      // println(lastUsagesPre.view.mapValues(_.map(_.name)).toMap)
      println(fn.name.value)
      println(lastUsagesPost.view.mapValues(_.map(_.name)).toMap)

      // param names don't need to be mangled because they're the first occurrence
      val params = fn.params
        .map(param => s"${typeRefToC(param.typ.name)} ${param.name.value}")
        .mkString(", ")
      val paramsSetup = fn.params.map { param =>
        if (unusedParams(param.name.value)) ""
        else incrRc(param.name.value, bindings.getType(param.typ))
      }.mkString("\n")

      val (bodySetup, body, bodyTeardown) = exprToC(fn.body)
      val resVar = newVar("ret")
      val typeToC = typeRefToC(fn.returnType.name)

      val signature = s"$typeToC ${mangleFnName(fn.name.value)}($params)"

      val numSccs = cycles.sccs.size
      val allocPcrBuckets =
        if (fn.name.value == "main")
          indent(1)(s"""|pcrBuckets = calloc(sizeof(void *), $numSccs);
                        |numSccs = $numSccs;""".stripMargin)
        else ""

      val triggerGC =
        if (fn.name.value == "main")
          indent(1)("""|processAllPCRs();
                       |free(pcrBuckets);""".stripMargin)
        else ""

      val decl = s"$signature;"
      val impl = s"""|$signature {
                     |$allocPcrBuckets
                     |${indent(1)(paramsSetup)}
                     |${indent(1)(bodySetup)}
                     |  $typeToC $resVar = $body;
                     |${indent(1)(bodyTeardown)}
                     |$triggerGC
                     |  return $resVar;
                     |}""".stripMargin
      (decl, impl)
    }

    /** Go through an expression backwards, finding the last usage of every
      * variable. These last usages are added to `lastUsagesPre` and
      * `lastUsagesPost`.
      *
      * @param vars
      *   Variables that so far have been unused ("so far" while moving
      *   backwards, not forwards)
      * @return
      *   The remaining variables that weren't used in this expression
      */
    private def findLastUsages(expr: Expr, vars: Set[VarDef])(using
        bindings: Bindings
    ): Set[VarDef] = {
      expr match {
        case IntLiteral(_, _) | StringLiteral(_, _) => vars
        case VarRef(name, span) =>
          val varDef = bindings.vars(name)
          if (vars.contains(varDef)) {
            lastUsagesPost(expr) = Set(varDef)
            vars - varDef
          } else { vars }
        case SetFieldExpr(lhsObj, lhsField, value, span) =>
          val varDef = bindings.vars(lhsObj.value)
          if (vars.contains(varDef)) {
            lastUsagesPost(expr) = Set(varDef)
            findLastUsages(value, vars - varDef)
          } else { findLastUsages(value, vars) }
        case FieldAccess(obj, field, typ) => findLastUsages(obj, vars)
        case BinExpr(lhs, op, rhs, typ) =>
          val remVars = findLastUsages(rhs, vars)
          findLastUsages(lhs, remVars)
        case FnCall(fnName, args, resolvedFn, typ, span) => args
            .foldRight(vars)(findLastUsages(_, _))
        case CtorCall(ctorName, values, span) => values
            .foldRight(vars) { case ((_, expr), vars) =>
              findLastUsages(expr, vars)
            }
        case expr @ LetExpr(name, value, body, span) =>
          val varType = typer.types(value)
          val varDef = VarDef.Let(expr, varType)
          val newBindings = bindings.withVar(name.value, varDef)
          var bodyRemVars =
            findLastUsages(body, vars + varDef)(using newBindings)
          if (bodyRemVars.contains(varDef)) {
            unusedVars += varDef
            bodyRemVars -= varDef
          }
          findLastUsages(value, bodyRemVars)
        case IfExpr(cond, thenBody, elseBody, span) =>
          val thenRemVars = findLastUsages(thenBody, vars)
          val elseRemVars = findLastUsages(elseBody, vars)
          lastUsagesPre(thenBody) = thenRemVars -- elseRemVars
          lastUsagesPre(elseBody) = elseRemVars -- thenRemVars
          findLastUsages(cond, thenRemVars & elseRemVars)
        case matchExpr @ MatchExpr(obj, arms, armsSpan) =>
          val objType = typer.types(obj).asInstanceOf[TypeDef]

          val armVars = arms.map {
            case arm @ MatchArm(
                  pat @ MatchPattern(ctorName, patBindings),
                  body,
                  _
                ) =>
              val newBindings = bindings.enterPattern(matchExpr, pat, objType)
              val newVars = patBindings.map { case (_, varName) =>
                newBindings.vars(varName.value)
              }.toSet
              val remVars =
                findLastUsages(body, vars | newVars)(using newBindings)

              unusedVars ++= newVars -- remVars

              arm -> remVars
          }.toMap

          for (arm <- arms) {
            val remVars = armVars(arm)
            val otherArms = arms.filter(_ != arm)
            val otherVars = otherArms.map(armVars).reduce(_ | _)
            lastUsagesPre(arm.body) = remVars -- otherVars
          }

          val commonUnused = armVars.values.reduce(_ | _)
          findLastUsages(obj, commonUnused)
      }
    }

    /** @return
      *   A tuple containing the C code for setup, the C code for the actual
      *   expression, and the C code for teardown (decrementing reference
      *   counts). The first string is necessary for let expressions, match
      *   expressions, and the like, where in C, you need some statements to
      *   define a variable before you can use it in an expression
      */
    private def exprToC(
        expr: Expr
    )(using bindings: Bindings): (String, String, String) = {
      val (setup, toC, teardown) = expr match {
        case IntLiteral(value, _) => ("", value.toString, "")
        case StringLiteral(value, _) =>
          ("", s"\"${value.replace("\"", "\\\"")}\"", "")
        case VarRef(name, _) =>
          ("", mangledVars.getOrElse(bindings.vars(name), name), "")
        case SetFieldExpr(obj, field, value, span) =>
          val (valSetup, valTranslated, valTeardown) = exprToC(value)
          val fieldAccess = s"${obj.value}->${field.value}"
          val objType = bindings.vars(obj.value).typ.asInstanceOf[TypeDef]
          val fieldDef = objType.cases.head.fields
            .find(_.name.value == field.value).get
          val fieldType = bindings.types(fieldDef.typ.name)
          val oldValue = newVar("oldValue")
          (
            s"""|${typeRefToC(fieldType.name)} $oldValue = $fieldAccess;
                |$valSetup
                |$fieldAccess = $valTranslated;
                |${incrRc(fieldAccess, fieldType)}
                |${decrRc(oldValue, fieldType)}""".stripMargin,
            fieldAccess,
            valTeardown
          )
        case BinExpr(lhs, op, rhs, typ) =>
          val (lhsSetup, lhsTranslated, lhsTeardown) = exprToC(lhs)
          val (rhsSetup, rhsTranslated, rhsTeardown) = exprToC(rhs)
          if (op.value == BinOp.Seq) {
            val execLhs = typer.types(lhs) match {
              case td: TypeDef if !lhs.isInstanceOf[SetFieldExpr] =>
                s"drop((void *) $lhsTranslated, (void *) ${Decrementer.name(td)});"
              case _ => lhs match {
                  case _: SetFieldExpr                          => ""
                  case call: FnCall if call.fnName.value == "c" => ""
                  case _ => s"$lhsTranslated;"
                }
            }
            (
              s"$lhsSetup\n$execLhs\n$lhsTeardown\n$rhsSetup",
              rhsTranslated,
              rhsTeardown
            )
          } else {
            (
              s"$lhsSetup\n$rhsSetup",
              s"$lhsTranslated ${op.value.text} $rhsTranslated",
              s"$lhsTeardown\n$rhsTeardown"
            )
          }
        case letExpr @ LetExpr(name, value, body, _) =>
          val (valueSetup, valueToC, valueTeardown) = exprToC(value)
          val typ = typer.types(value)
          val shouldMangle = bindings.vars.contains(name.value)
          val varDef = VarDef.Let(letExpr, typ)
          val newBindings = bindings.withVar(name.value, varDef)
          val mangledName =
            if (shouldMangle) newMangledVar(name.value) else name.value
          if (shouldMangle) {
            mangledVars.put(bindings.vars(name.value), mangledName)
          }
          val (letSetup, letTeardown) = addBinding(mangledName, valueToC, typ)
          val (bodySetup, bodyToC, bodyTeardown) =
            exprToC(body)(using newBindings)

          val decr =
            if (unusedVars.contains(varDef)) decrRc(mangledName, typ) else ""
          (
            s"$valueSetup\n$letSetup\n$valueTeardown\n$decr\n$bodySetup",
            bodyToC,
            s"$bodyTeardown\n$letTeardown"
          )
        case IfExpr(cond, thenBody, elseBody, _) =>
          val (condSetup, condC, condTeardown) = exprToC(cond)
          val (thenSetup, thenC, thenTeardown) = exprToC(thenBody)
          val (elseSetup, elseC, elseTeardown) = exprToC(elseBody)

          val typ = typeRefToC(typer.types(expr).name)
          val resVar = newVar("ifres")

          val setup = s"""|$condSetup
                          |$typ $resVar;
                          |if ($condC) {
                          |${indent(1)(condTeardown)}
                          |${indent(1)(thenSetup)}
                          |  $resVar = $thenC;
                          |${indent(1)(thenTeardown)}
                          |} else {
                          |${indent(1)(condTeardown)}
                          |${indent(1)(elseSetup)}
                          |  $resVar = $elseC;
                          |${indent(1)(elseTeardown)}
                          |}""".stripMargin

          (setup, resVar, "")
        case FieldAccess(obj, field, _) =>
          val (objSetup, objToC, objTeardown) = exprToC(obj)
          (objSetup, s"$objToC->${field.value}", objTeardown)
        case FnCall(fnName, args, _, _, _) =>
          if (fnName.value == "c") {
            (args.head.asInstanceOf[StringLiteral].value, "0", "")
          } else {
            val (setups, argsToC, teardowns) = args.map(exprToC).unzip3
            (
              setups.mkString("\n"),
              s"${mangleFnName(fnName.value)}(${argsToC.mkString(", ")})",
              teardowns.mkString("\n")
            )
          }
        case CtorCall(ctorNameSpanned, values, span) =>
          val (typ, variant) = bindings.ctors(ctorNameSpanned.value)
          val (setups, args, teardowns) = values.toSeq
            .sortBy((fieldName, _) => fieldName.value)
            .map((_, value) => exprToC(value)).unzip3
          (
            setups.mkString("\n"),
            s"${ctorName(variant)}(${args.mkString(", ")})",
            teardowns.mkString("\n")
          )
        case matchExpr @ MatchExpr(obj, arms, _) =>
          val (objSetup, objToC, objTeardown) = exprToC(obj)
          val objType = typer.types(obj).asInstanceOf[TypeDef]
          val objVar = newVar("matchobj")
          val resType = typer.types(expr)
          val resVar = newVar("matchres")

          val armsToC = arms.map {
            case MatchArm(pat @ MatchPattern(ctorName, patBindings), body, _) =>
              // TODO decrement unused variables immediately
              val variant = objType.cases.find(_.name.value == ctorName.value)
                .get
              val oldBindings = bindings
              given newBindings: Bindings = oldBindings
                .enterPattern(matchExpr, pat, objType)
              val (bindingSetups, bindingTeardowns) = patBindings
                .map { (fieldName, varName) =>
                  val fieldNameMangled =
                    if (objType.hasCommonField(fieldName.value)) fieldName.value
                    else mangledFieldName(variant, fieldName.value)
                  val shouldMangle = oldBindings.vars.contains(varName.value)
                  val mangledVarName =
                    if (shouldMangle) newMangledVar(varName.value)
                    else varName.value
                  if (shouldMangle) {
                    mangledVars
                      .put(newBindings.vars(varName.value), mangledVarName)
                  }
                  addBinding(
                    mangledVarName,
                    s"$objVar->$fieldNameMangled",
                    newBindings.types(
                      variant.fields.find(_.name.value == fieldName.value).get
                        .typ.name
                    )
                  )
                }.unzip
              val (bodySetup, bodyToC, bodyTeardown) =
                exprToC(body)(using newBindings)
              s"""|case ${tagName(variant.name.value)}:
                  |${indent(1)(bindingSetups.mkString("\n"))}
                  |${indent(1)(bodySetup)}
                  |  $resVar = $bodyToC;
                  |${indent(1)(bodyTeardown)}
                  |${indent(1)(bindingTeardowns.mkString("\n"))}
                  |  break;""".stripMargin
          }

          val setup = s"""|$objSetup
                          |${typeRefToC(objType.name)} $objVar = $objToC;
                          |${typeRefToC(resType.name)} $resVar;
                          |switch ($objVar->kind) {
                          |${armsToC.mkString("\n")}
                          |}
                          |${incrRc(resVar, resType)}
                          |$objTeardown""".stripMargin
          val teardown = decrRc(resVar, resType)

          (setup, resVar, teardown)
      }

      val setupDecrs = lastUsagesPre.getOrElse(expr, Set.empty).map { varDef =>
        decrRc(mangledVars.getOrElse(varDef, varDef.name), varDef.typ)
      }.mkString("", "\n", "\n")
      val teardownDecrs = lastUsagesPost.getOrElse(expr, Set.empty)
        .map { varDef =>
          decrRc(mangledVars.getOrElse(varDef, varDef.name), varDef.typ)
        }.mkString("\n", "\n", "")

      if (lastUsagesPre.getOrElse(expr, Set.empty).nonEmpty) {
        println(
          s"last usage pre: ${lastUsagesPre(expr).map(_.name)}, expr: $expr"
        )
      }
      if (lastUsagesPost.getOrElse(expr, Set.empty).nonEmpty) {
        println(
          s"last usage post: ${lastUsagesPost(expr).map(_.name)}, expr: $expr"
        )
      }

      (setupDecrs + setup, toC, teardown + teardownDecrs)
    }

    private def newMangledVar(baseName: String): String = {
      baseName + "$" + mangledVars.keySet.filter(_.name == baseName).size
    }

    private def addBinding(
        varName: String,
        value: String,
        typ: Type
    ): (String, String) = {
      val setup = s"""|${typeRefToC(typ.name)} ${varName} = $value;
                      |${incrRc(varName, typ)}""".stripMargin
      (setup, "")
    }

    /** Create a variable name that hopefully won't conflict with any other
      * variables
      */
    private def newVar(prefix: String): String = {
      val res = prefix + "$" + resVarCounter
      resVarCounter += 1
      res
    }
  }
}
