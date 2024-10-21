package fred

import scala.util.Random
import scala.collection.mutable

object Translator {
  private val KindField = "kind"
  private val RcField = "rc"
  private val This = "this"
  private val Black = "kBlack"
  private val Gray = "kGray"
  private val White = "kWhite"
  private val ColorField = "color"
  private val CommonIncludes = s"""|#include <stdlib.h>
                                   |#include <stdio.h>
                                   |
                                   |enum Color { $Black, $Gray, $White };
                                   |""".stripMargin

  private val NoMangleFns = Set("main", "printf")

  def toC(file: ParsedFile)(using typer: Typer): String = {
    given Bindings = Bindings.fromFile(file)
    val helper = Helper(typer)
    val (genDecls, genImpls) =
      List(Decrementer, MarkGray, Scan, ScanBlack, CollectWhite)
        .flatMap(gen => file.typeDefs.map(td => (gen.decl(td), gen.impl(td))))
        .unzip
    val (fnDecls, fnImpls) = file.fns.map(helper.fnToC).unzip
    val generated =
      file.typeDefs.map(helper.translateType).mkString("", "\n", "\n")
        + genDecls.mkString("", "\n", "\n")
        + fnDecls.mkString("", "\n", "\n")
        + genImpls.mkString("", "\n", "\n")
        + fnImpls.mkString("", "\n", "\n")
    CommonIncludes + generated
      .replaceAll(raw"\n(\s|\n)*\n", "\n")
      .strip() + "\n"
  }

  trait GeneratedFn(unmangledName: String) {
    def returnType: String

    def body(typ: TypeDef)(using Bindings): String

    def name(typ: TypeDef) = s"$$${unmangledName}_${typ.name}"

    private def sig(typ: TypeDef) =
      s"$returnType ${name(typ)}(${typeRefToC(typ.name)} $This)"

    def decl(typ: TypeDef) = s"${sig(typ)};"

    def impl(typ: TypeDef)(using Bindings) =
      s"${sig(typ)} {\n${indent(1)(body(typ))}\n}"
  }

  /** Generate the signature and implementation for the decrementer function
    */
  private object Decrementer extends GeneratedFn("decr") {
    override def returnType = "void"

    override def body(typ: TypeDef)(using bindings: Bindings): String = {
      // Cases for deleting the object
      val deleteCases = indent(1) {
        switch(This, typ) {
          case variant @ EnumCase(Spanned(ctorName, _), fields, _) =>
            fields
              .map { case FieldDef(_, fieldName, fieldType, _) =>
                val mangled = cFieldName(fieldName.value, typ, variant)
                s"${decrRc(s"$This->$mangled", bindings.types(fieldType.name))}"
              }
              .mkString("\n")
        }
      }

      s"""|if (--$This->$RcField == 0) {
          |$deleteCases
          |  free($This);
          |} else {
          |  // todo
          |}""".stripMargin
    }
  }

  private object MarkGray extends GeneratedFn("markGray") {
    override def returnType: String = "void"

    override def body(typ: TypeDef)(using bindings: Bindings): String = {
      val recMarks = indent(1) {
        switch(This, typ) { variant =>
          variant.fields
            .map { case FieldDef(_, fieldName, fieldTypeRef, _) =>
              val mangled = cFieldName(fieldName.value, typ, variant)
              bindings.types(fieldTypeRef.name) match {
                case fieldType: TypeDef =>
                  s"""|${decrRc(s"$This->$mangled", fieldType)}
                      |${MarkGray.name(fieldType)}($This->$mangled);""".stripMargin
                case _ => ""
              }
            }
            .mkString("\n")
        }
      }
      s"""|if ($This->$ColorField == $Gray) return;
          |$This->$ColorField = $Gray;
          |$recMarks""".stripMargin
    }
  }

  private object Scan extends GeneratedFn("scan") {
    override def returnType: String = "void"

    override def body(typ: TypeDef)(using bindings: Bindings): String = {
      val recScan =
        switch(This, typ) { variant =>
          variant.fields
            .map { case FieldDef(_, fieldName, fieldTypeRef, _) =>
              val mangled = cFieldName(fieldName.value, typ, variant)
              bindings.types(fieldTypeRef.name) match {
                case fieldType: TypeDef =>
                  s"${Scan.name(fieldType)}($This->$mangled);"
                case _ => ""
              }
            }
            .mkString("\n")
        }

      s"""|if ($This->$ColorField != $Gray) return;
          |if ($This->$RcField > 0) {
          |  ${ScanBlack.name(typ)}($This);
          |  return;
          |}
          |$This->$ColorField = $White;
          |$recScan""".stripMargin
    }
  }

  private object ScanBlack extends GeneratedFn("scanBlack") {
    override def returnType: String = "void"

    override def body(typ: TypeDef)(using bindings: Bindings): String = {
      val recScan = indent(1) {
        switch(This, typ) { variant =>
          variant.fields
            .map { case FieldDef(_, fieldName, fieldTypeRef, _) =>
              val mangled = cFieldName(fieldName.value, typ, variant)
              bindings.types(fieldTypeRef.name) match {
                case fieldType: TypeDef =>
                  s"""|${incrRc(s"$This->$mangled", fieldType)}
                      |${ScanBlack.name(fieldType)}($This->$mangled);""".stripMargin
                case _ => ""
              }
            }
            .mkString("\n")
        }
      }
      s"""|if ($This->$ColorField != $Black) {
          |  $This->$ColorField = $Black;
          |$recScan
          |}""".stripMargin
    }
  }

  private object CollectWhite extends GeneratedFn("collectWhite") {
    override def returnType: String = "void"

    override def body(typ: TypeDef)(using bindings: Bindings): String = {
      val rec = indent(1) {
        switch(This, typ) { variant =>
          variant.fields
            .map { case FieldDef(_, fieldName, fieldTypeRef, _) =>
              val mangled = cFieldName(fieldName.value, typ, variant)
              bindings.types(fieldTypeRef.name) match {
                case fieldType: TypeDef =>
                  s"${CollectWhite.name(fieldType)}($This->$mangled);"
                case _ => ""
              }
            }
            .mkString("\n")
        }
      }
      s"""|if ($This->$ColorField == $White) {
          |  $This->$ColorField = $Black;
          |$rec
          |  free($This);
          |}""".stripMargin
    }
  }

  private def switch(expr: String, typ: TypeDef)(
      createArm: EnumCase => String
  ): String = {
    val armsToC = typ.cases
      .map { variant =>
        s"""|case ${tagName(variant.name.value)}:
            |${indent(1)(createArm(variant))}
            |  break;""".stripMargin
      }
      .mkString("\n")
    s"""|switch ($expr->$KindField) {
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

  private def incrRc(expr: String, typ: Type) = {
    if (typ.isInstanceOf[TypeDef]) {
      s"$expr->$RcField ++;"
    } else {
      ""
    }
  }

  private def decrRc(expr: String, typ: Type) = {
    typ match {
      case td: TypeDef => s"${Decrementer.name(td)}($expr);"
      case _           => ""
    }
  }

  /** Get the name of this field in C (mangled if necessary)
    */
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
                       |  enum Color $ColorField;
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

    /** Returns code for the function's declaration and implementation
      */
    def fnToC(fn: FnDef)(using bindings: Bindings): (String, String) = {
      // param names don't need to be mangled because they're the first occurrence
      val params = fn.params
        .map(param => s"${typeRefToC(param.typ.name)} ${param.name.value}")
        .mkString(", ")
      val paramsSetup = fn.params
        .map(param => incrRc(param.name.value, bindings.getType(param.typ)))
        .mkString("\n")
      val paramsTeardown = fn.params
        .map(param => decrRc(param.name.value, bindings.getType(param.typ)))
        .mkString("\n")
      val (bodySetup, body, bodyTeardown) =
        exprToC(fn.body)(using bindings.enterFn(fn))
      val resVar = newVar("ret")
      val typeToC = typeRefToC(fn.returnType.name)

      val signature = s"$typeToC ${mangleFnName(fn.name.value)}($params)"

      val decl = s"$signature;"
      val impl = s"""|$signature {
                     |${indent(1)(paramsSetup)}
                     |${indent(1)(bodySetup)}
                     |  $typeToC $resVar = $body;
                     |${indent(1)(bodyTeardown)}
                     |${indent(1)(paramsTeardown)}
                     |  return $resVar;
                     |}""".stripMargin
      (decl, impl)
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
        case SetFieldExpr(obj, field, value, span) =>
          val (valSetup, valTranslated, valTeardown) = exprToC(value)
          (
            valSetup,
            s"${obj.value}->${field.value} = $valTranslated",
            valTeardown
          )
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
          val resVar = newVar("ifres")

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
          val resVar = newVar("ctorres")
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
              val fieldAccess =
                s"$resVar->${cFieldName(fieldName.value, typ, variant)}"
              s"""|$valueSetup
                  |$fieldAccess = $valueToC;
                  |${incrRc(fieldAccess, fieldType)}
                  |$valueTeardown""".stripMargin
            }
            .mkString("\n")
          val setup =
            s"""|${typeRefToC(typ.name)} $resVar = malloc(sizeof (struct ${typ.name}));
                |$resVar->$RcField = 0;
                |$resVar->$KindField = ${tagName(ctorName.value)};
                |$valueSetups""".stripMargin
          (setup, resVar, "")
        case matchExpr @ MatchExpr(obj, arms, _) =>
          val (objSetup, objToC, objTeardown) = exprToC(obj)
          val objType = typer.types(obj).asInstanceOf[TypeDef]
          val objVar = newVar("matchobj")
          val resType = typer.types(expr)
          val resVar = newVar("matchres")

          val armsToC = arms.map {
            case MatchArm(pat @ MatchPattern(ctorName, patBindings), body, _) =>
              val variant =
                objType.cases.find(_.name.value == ctorName.value).get
              val oldBindings = bindings
              given newBindings: Bindings =
                oldBindings.enterPattern(matchExpr, pat, objType)
              val (bindingSetups, bindingTeardowns) =
                patBindings.map { (fieldName, varName) =>
                  val fieldNameMangled =
                    if (objType.hasCommonField(fieldName.value)) fieldName.value
                    else mangledFieldName(variant, fieldName.value)
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
                    s"$objVar->$fieldNameMangled",
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
    private def newVar(prefix: String): String = {
      val res = prefix + "$" + resVarCounter
      resVarCounter += 1
      res
    }
  }
}
