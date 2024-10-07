package fred

import scala.util.Random
import scala.collection.mutable

object Translator {
  private val KindField = "kind"
  private val RcField = "rc"

  def toC(file: ParsedFile)(using typer: Typer): String = {
    given Bindings = Bindings.fromFile(file)
    val helper = Helper(typer)
    val generated =
      file.typeDefs.map(helper.translateType).mkString("\n") + "\n" + file.fns
        .map(helper.fnToC)
        .mkString("\n")
    generated.replaceAll(raw"\n\s*\n", "\n")
  }

  private class Helper(typer: Typer) {

    /** Contains a mapping of mangled field names for every type
      */
    val mangledFieldsFor = mutable.Map.empty[TypeDef, Map[String, String]]

    val mangledVars = mutable.Map.empty[VarDef, String]

    private var resVarCounter = 0

    def translateType(typ: TypeDef): String = {
      val name = typ.name
      val tagNames =
        typ.cases.map(enumCase => tagName(enumCase.name.value)).mkString(", ")
      val tagEnum = s"enum ${name}_kind { ${tagNames} };"

      val mangledFields = mutable.Map.empty[String, String]

      val commonFields =
        typ.cases.head.fields.filter { field =>
          typ.hasCommonField(field.name.value)
        }
      val commonFieldsToC = commonFields
        .map { field =>
          s"  ${typeRefToC(field.typ.name)} ${field.name.value};"
        }
        .mkString("\n")
      mangledFields ++= commonFields.map(field =>
        field.name.value -> field.name.value
      )

      val cases = typ.cases
        .map { enumCase =>
          val variantFields = enumCase.fields
            .filter { field =>
              commonFields.forall(_.name.value != field.name.value)
            }

          mangledFields ++= variantFields.map(field =>
            field.name.value -> mangledFieldName(enumCase, field.name.value)
          )

          val fields = variantFields
            .map { field =>
              s"${typeRefToC(field.typ.name)} ${mangledFieldName(enumCase, field.name.value)};"
            }
            .mkString(" ")
          s"struct { $fields };"
        }
        .mkString("\n    ")

      mangledFieldsFor.put(typ, mangledFields.toMap)

      val struct = s"""|struct $name {
                       |  int $RcField;
                       |  enum ${name}_kind $KindField;
                       |$commonFieldsToC
                       |  union {
                       |    $cases
                       |  };
                       |};""".stripMargin
      tagEnum + "\n" + struct
    }

    private def enumCaseToC(enumCase: EnumCase) = {
      val fields = enumCase.fields
        .map { field =>
          s"${typeRefToC(field.typ.name)} ${mangledFieldName(enumCase, field.name.value)};"
        }
        .mkString(" ")
      s"struct { $fields };"
    }

    def fnToC(fn: FnDef)(using bindings: Bindings) = {
      // todo increment refcount for parameters

      // param names don't need to be mangled because they're the first occurrence
      val params = fn.params
        .map(param => s"${typeRefToC(param.typ.name)} ${param.name.value}")
        .mkString(", ")
      val (bodySetup, body, bodyTeardown) =
        exprToC(fn.body)(using bindings.enterFn(fn))
      val typeToC = typeRefToC(fn.returnType.name)
      val resVar = s"fn${newVar()}"
      s"""|$typeToC ${mangleFnName(fn.name.value)}($params) {
          |${indent(1)(bodySetup)}
          |  $typeToC $resVar = $body;
          |${indent(1)(bodyTeardown)}
          |  return $resVar;
          |}""".stripMargin
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
      expr match {
        case IntLiteral(value, _) => ("", value.toString, "")
        case StringLiteral(value, _) =>
          ("", s"\"${value.replace("\"", "\\\"")}\"", "")
        case VarRef(name, _, _) =>
          ("", mangledVars.getOrElse(bindings.vars(name), name), "")
        case BinExpr(lhs, op, rhs, typ) =>
          val (lhsSetup, lhsTranslated, lhsTeardown) = exprToC(lhs)
          val (rhsSetup, rhsTranslated, rhsTeardown) = exprToC(rhs)
          val setup = s"$lhsSetup\n$rhsSetup"
          val teardown = s"$lhsTeardown\n$rhsTeardown"
          if (op.value == BinOp.Seq) {
            (s"$setup\n$lhsTranslated;", rhsTranslated, teardown)
          } else {
            (setup, s"$lhsTranslated ${op.value.text} $rhsTranslated", teardown)
          }
        case letExpr @ LetExpr(name, value, body, _) =>
          val (valueSetup, valueToC, valueTeardown) = exprToC(value)
          val typ = typer.types(value)
          val shouldMangle = bindings.vars.contains(name.value)
          val newBindings =
            bindings.withVar(name.value, VarDef.Let(letExpr, typ))
          val mangledName =
            if (shouldMangle) newMangledVar(name.value) else name.value
          if (shouldMangle) {
            mangledVars.put(
              bindings.vars(name.value),
              newMangledVar(name.value)
            )
          }
          val (letSetup, letTeardown) = addBinding(mangledName, valueToC, typ)
          val (bodySetup, bodyToC, bodyTeardown) =
            exprToC(body)(using newBindings)

          (
            s"$valueSetup\n$letSetup\n$bodySetup",
            bodyToC,
            s"$valueTeardown\n$bodyTeardown\n$letTeardown"
          )
        case IfExpr(cond, thenBody, elseBody, _) =>
          val (condSetup, condC, condTeardown) = exprToC(cond)
          val (thenSetup, thenC, thenTeardown) = exprToC(thenBody)
          val (elseSetup, elseC, elseTeardown) = exprToC(elseBody)

          val typ = typeRefToC(typer.types(expr).name)
          val resVar = s"if${newVar()}"

          val setup = s"""|$condSetup
                          |$typ $resVar;
                          |if ($condC) {
                          |${indent(1)(thenSetup)}
                          |  $resVar = $thenC;
                          |} else {
                          |${indent(1)(elseSetup)}
                          |  $resVar = $elseC;
                          |}""".stripMargin

          (setup, resVar, condTeardown)
        case FieldAccess(obj, field, _) =>
          val (objSetup, objToC, objTeardown) = exprToC(obj)
          (objSetup, s"$objToC->${field.value}", objTeardown)
        case FnCall(fnName, args, _, _, _) =>
          val (setups, argsToC, teardowns) = args.map(exprToC).unzip3
          (
            setups.mkString("\n"),
            s"${mangleFnName(fnName.value)}(${argsToC.mkString(", ")})",
            teardowns.mkString("\n")
          )
        case CtorCall(ctorName, values, span) =>
          val (typ, variant) = bindings.ctors(ctorName.value)
          val resVar = s"ctor${newVar()}"
          val valueSetups = values
            .map { case (fieldName, value) =>
              val fieldType = bindings.types(
                variant.fields
                  .find(_.name.value == fieldName.value)
                  .get
                  .typ
                  .name
              )
              val (valueSetup, valueToC, valueTeardown) = exprToC(value)
              val mangledName =
                if (typ.hasCommonField(fieldName.value)) fieldName.value
                else mangledFieldName(variant, fieldName.value)
              val fieldAccess = s"$resVar->$mangledName"
              s"""|$valueSetup
                  |$fieldAccess = $valueToC;
                  |${incrRc(fieldAccess, fieldType)}
                  |$valueTeardown""".stripMargin
            }
            .mkString("\n")
          val setup =
            s"""|${typeRefToC(typ.name)} $resVar = malloc(sizeof (struct ${typ.name}));
                |$resVar->$KindField = ${tagName(ctorName.value)};
                |$valueSetups""".stripMargin
          (setup, resVar, "")
        case matchExpr @ MatchExpr(obj, arms, _) =>
          val (objSetup, objToC, objTeardown) = exprToC(obj)
          val objType = typer.types(obj).asInstanceOf[TypeDef]
          val objVar = s"matchobj${newVar()}"
          val resType = typer.types(expr)
          val resVar = s"matchres${newVar()}"

          val armsToC = arms.map {
            case MatchArm(pat @ MatchPattern(ctorName, patBindings), body, _) =>
              val variant =
                objType.cases.find(_.name.value == ctorName.value).get
              val oldBindings = bindings
              given newBindings: Bindings =
                oldBindings.enterPattern(matchExpr, pat, objType)
              val (bindingSetups, bindingTeardowns) =
                patBindings.map { (fieldName, varName) =>
                  val mangledFieldName =
                    if (objType.hasCommonField(fieldName.value)) fieldName.value
                    else this.mangledFieldName(variant, fieldName.value)
                  val shouldMangle = oldBindings.vars.contains(varName.value)
                  val mangledVarName =
                    if (shouldMangle) newMangledVar(varName.value)
                    else varName.value
                  if (shouldMangle) {
                    mangledVars.put(
                      newBindings.vars(varName.value),
                      mangledVarName
                    )
                  }
                  addBinding(
                    mangledVarName,
                    s"$objVar->$mangledFieldName",
                    newBindings.types(
                      variant.fields
                        .find(_.name.value == fieldName.value)
                        .get
                        .typ
                        .name
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
                          |switch ($objVar->$KindField) {
                          |${armsToC.mkString("\n")}
                          |}
                          |${incrRc(resVar, resType)}
                          |$objTeardown""".stripMargin
          val teardown = decrRc(resVar, resType)

          (setup, resVar, teardown)
      }
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
      val teardown = decrRc(varName, typ)
      (setup, teardown)
    }

    private def incrRc(expr: String, typ: Type) = {
      if (typ.isInstanceOf[TypeDef]) {
        s"$expr->$RcField ++;"
      } else {
        ""
      }
    }

    private def decrRc(expr: String, typ: Type) = {
      if (typ.isInstanceOf[TypeDef]) {
        s"""|if (--$expr->$RcField == 0) {
            |  free($expr);
            |}""".stripMargin
      } else {
        ""
      }
    }

    private def merge(
        setupsOrTeardowns: Option[String]*
    ): Option[String] = {
      setupsOrTeardowns.reduceLeft {
        case (Some(first), Some(second)) => Some(s"$first\n$second")
        case (Some(teardown), None)      => Some(teardown)
        case (None, Some(teardown))      => Some(teardown)
        case (None, None)                => None
      }
    }

    /** Create a variable name that hopefully won't conflict with any other
      * variables
      */
    private def newVar(): String = {
      val res = "res$" + resVarCounter
      resVarCounter += 1
      res
    }

    private def typeRefToC(typeName: String) = {
      typeName match {
        case "int" => "int"
        case "str" => "char*"
        case name  => s"struct $name*"
      }
    }

    private def mangleFnName(fnName: String) =
      if (fnName != "main") "fn$" + fnName else "main"

    /** Field names need to be mangled so that multiple cases can have fields
      * with the same name
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

    /** Apply `2 * level` spaces of indentation to every line in the given
      * string
      */
    private def indent(level: Int)(s: String): String = {
      s.split("\n").map(line => "  " * level + line).mkString("\n")
    }
  }
}
