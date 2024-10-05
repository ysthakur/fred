package fred

import scala.util.Random

object Translator {
  private val KindField = "kind"
  private val RcField = "rc"

  def toC(file: ParsedFile)(using Typer): String = {
    given Bindings = Bindings.fromFile(file)
    file.typeDefs.map(typeToC).mkString("\n") + "\n" + file.fns
      .map(fnToC)
      .mkString("\n")
  }

  private def typeToC(typ: TypeDef) = {
    val name = typ.name
    val tagNames =
      typ.cases.map(enumCase => tagName(enumCase.name.value)).mkString(", ")
    val tagEnum = s"enum ${name}_kind { ${tagNames} };"

    val commonFields =
      typ.cases.head.fields.filter { field =>
        typ.hasCommonField(field.name.value)
      }
    val commonFieldsToC = commonFields
      .map { field =>
        s"  ${typeRefToC(field.typ.name)} ${field.name.value};"
      }
      .mkString("\n")

    val cases = typ.cases
      .map { enumCase =>
        val fields = enumCase.fields
          .filter { field =>
            commonFields.forall(_.name.value != field.name.value)
          }
          .map { field =>
            s"${typeRefToC(field.typ.name)} ${mangledFieldName(enumCase, field.name.value)};"
          }
          .mkString(" ")
        s"struct { $fields };"
      }
      .mkString("\n    ")
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

  private def fnToC(fn: FnDef)(using Bindings, Typer) = {
    // todo increment refcount for parameters
    val params = fn.params
      .map(param => s"${typeRefToC(param.typ.name)} ${param.name.value}")
      .mkString(", ")
    val (bodySetup, body, bodyTeardown) = exprToC(fn.body)
    val typeToC = typeRefToC(fn.returnType.name)
    val resVar = newVar(fn.hashCode())
    s"""|$typeToC ${mangleFnName(fn.name.value)}($params) {
        |${indent(1)(bodySetup)}
        |  $typeToC $resVar = $body;
        |${indent(1)(bodyTeardown)}
        |  return $resVar;
        |}""".stripMargin
  }

  /** @return
    *   A tuple containing the C code for setup, the C code for the actual
    *   expression, and the C code for teardown (decrementing reference counts).
    *   The first string is necessary for let expressions, match expressions,
    *   and the like, where in C, you need some statements to define a variable
    *   before you can use it in an expression
    */
  private def exprToC(
      expr: Expr
  )(using
      bindings: Bindings,
      typer: Typer
  ): (String, String, String) = {
    expr match {
      case IntLiteral(value, _) => ("", value.toString, "")
      case StringLiteral(value, _) =>
        ("", s"\"${value.replace("\"", "\\\"")}\"", "")
      case VarRef(name, _, _) => ("", name, "")
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
        val (letSetup, letTeardown) = addBinding(name.value, valueToC, typ)
        val (bodySetup, bodyToC, bodyTeardown) = exprToC(body)(using
          bindings.withVar(name.value, VarDef.Let(letExpr, typ))
        )
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
        val resVar = newVar(expr.hashCode())

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
        val resVar = newVar(expr.hashCode())
        val valueSetups = values
          .map { case (fieldName, value) =>
            val fieldType = bindings.types(
              variant.fields.find(_.name.value == fieldName.value).get.typ.name
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
      case MatchExpr(obj, arms, _) =>
        val (objSetup, objToC, objTeardown) = exprToC(obj)
        val objType = typer.types(obj).asInstanceOf[TypeDef]
        val objVar = newVar(obj.hashCode())
        val resType = typer.types(expr)
        val resVar = newVar(expr.hashCode())

        val armsToC = arms.map {
          case MatchArm(MatchPattern(ctorName, patBindings), body, _) =>
            val variant = objType.cases.find(_.name.value == ctorName.value).get
            val (bindingSetups, bindingTeardowns) =
              patBindings.map { (fieldName, varName) =>
                val mangledName =
                  if (objType.hasCommonField(fieldName.value)) fieldName.value
                  else mangledFieldName(variant, fieldName.value)
                addBinding(
                  varName.value,
                  s"$objVar->$mangledName",
                  bindings.types(
                    variant.fields
                      .find(_.name.value == fieldName.value)
                      .get
                      .typ
                      .name
                  )
                )
              }.unzip
            val (bodySetup, bodyToC, bodyTeardown) = exprToC(body)
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
  private def newVar(seed: Int): String = {
    "var$" + seed.toHexString // Random().alphanumeric.take(5).mkString
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

  /** Apply `2 * level` spaces of indentation to every line in the given string
    */
  private def indent(level: Int)(s: String): String = {
    s.split("\n").map(line => "  " * level + line).mkString("\n")
  }
}
