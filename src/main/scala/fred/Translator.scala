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
    val params = fn.params
      .map(param => s"${typeRefToC(param.typ.name)} ${param.name.value}")
      .mkString(", ")
    val (bodySetup, body, bodyTeardown) = exprToC(fn.body)
    val typeToC = typeRefToC(fn.returnType.name)
    val resVar = newVar(fn.hashCode())
    s"""|$typeToC ${fn.name.value} ($params) {
        |${indent(1)(bodySetup.getOrElse(""))}
        |  $typeToC $resVar = $body;
        |${indent(1)(bodyTeardown.getOrElse(""))}
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
  ): (Option[String], String, Option[String]) = {
    expr match {
      case IntLiteral(value, _) => (None, value.toString, None)
      case StringLiteral(value, _) =>
        (None, s"\"${value.replace("\"", "\\\"")}\"", None)
      case VarRef(name, _, _) => (None, name, None)
      case BinExpr(lhs, op, rhs, typ) =>
        val (lhsSetup, lhsTranslated, lhsTeardown) = exprToC(lhs)
        val (rhsSetup, rhsTranslated, rhsTeardown) = exprToC(rhs)
        val toC = if (op.value == BinOp.Seq) {
          s"($lhsTranslated, $rhsTranslated)"
        } else {
          s"$lhsTranslated ${op.value.text} $rhsTranslated"
        }
        (merge(lhsSetup, rhsSetup), toC, merge(lhsTeardown, rhsTeardown))
      case letExpr @ fred.LetExpr(name, value, body, _) =>
        val (valueSetup, valueToC, valueTeardown) = exprToC(value)
        val typ = typer.types(value)
        val setup = s"""|${typeRefToC(typ.name)} ${name.value} = $valueToC;
                        |""".stripMargin
        val (bodySetup, bodyToC, bodyTeardown) = exprToC(body)(using
          bindings.withVar(name.value, VarDef.Let(letExpr, typ))
        )
        val teardown = ""
        (
          merge(merge(valueSetup, Some(setup)), bodySetup),
          bodyToC,
          merge(merge(valueTeardown, bodyTeardown), Some(teardown))
        )
      case fred.IfExpr(cond, thenBody, elseBody, _) =>
        val (condSetup, condC, condTeardown) = exprToC(cond)
        val (thenSetup, thenC, thenTeardown) = exprToC(thenBody)
        val (elseSetup, elseC, elseTeardown) = exprToC(elseBody)

        val typ = typeRefToC(typer.types(expr).name)
        val resVar = newVar(expr.hashCode())

        val setup = s"""|$condSetup
                        |$typ $resVar;
                        |if ($condC) {
                        |${indent(1)(thenSetup.getOrElse(""))}
                        |  $resVar = $thenC;
                        |} else {
                        |${indent(1)(elseSetup.getOrElse(""))}
                        |  $resVar = $elseC;
                        |}""".stripMargin

        (merge(condSetup, Some(setup)), resVar, condTeardown)
      case FieldAccess(obj, field, _) =>
        val (objSetup, objToC, objTeardown) = exprToC(obj)
        (objSetup, s"$objToC->${field.value}", objTeardown)
      case FnCall(fnName, args, _, _, _) =>
        val (setups, argsToC, teardowns) = args.map(exprToC).unzip3
        (
          merge(setups*),
          s"${fnName.value}(${argsToC.mkString(", ")})",
          merge(teardowns*)
        )
      case MatchExpr(obj, arms, _) =>
        val (objSetup, objToC, objTeardown) = exprToC(obj)
        val objType = typer.types(obj).asInstanceOf[TypeDef]
        val objVar = newVar(obj.hashCode())
        val resType = typer.types(expr)
        val resVar = newVar(expr.hashCode())

        val armsToC = arms.map { case MatchArm(pat, body, _) => ??? }

        val setup = s"""|${typeRefToC(objType.name)} $objVar = $objToC;
                        |${typeRefToC(resType.name)} $resVar;
                        |switch ($objVar->$KindField) {
                        |}""".stripMargin
        val teardown = s""""""

        (merge(objSetup, Some(setup)), ???, objTeardown)
    }
  }

  private def incrRc(expr: String) = s"$expr->$RcField ++;"

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
