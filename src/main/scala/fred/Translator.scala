package fred

import scala.util.Random

object Translator {
  private val KindField = "kind"

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
    val cases = typ.cases.map(enumCaseToC).mkString("\n    ")
    val struct = s"""|struct $name {
                     |  enum ${name}_kind $KindField;
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
    val (bodySetup, body) = exprToC(fn.body)
    s"""|${typeRefToC(fn.returnType.name)} ${fn.name.value} ($params) {
        |${indent(1)(bodySetup)}
        |  return $body;
        |}""".stripMargin
  }

  /** @return
    *   A tuple containing the C code for setup and the C code for the actual
    *   expression. The first string is necessary for let expressions, match
    *   expressions, and the like, where in C, you need some statements to
    *   define a variable before you can use it in an expression
    */
  private def exprToC(
      expr: Expr
  )(using bindings: Bindings, typer: Typer): (String, String) = {
    expr match {
      case IntLiteral(value, _) => ("", value.toString)
      case StringLiteral(value, _) =>
        ("", s"\"${value.replace("\"", "\\\"")}\"")
      case VarRef(name, _, _) => ("", name)
      case BinExpr(lhs, op, rhs, typ) =>
        val (lhsSetup, lhsTranslated) = exprToC(lhs)
        val (rhsSetup, rhsTranslated) = exprToC(rhs)
        val setup = s"$lhsSetup\n$rhsSetup"
        val toC = if (op.value == BinOp.Seq) {
          s"($lhsTranslated, $rhsTranslated)"
        } else {
          s"$lhsTranslated ${op.value.text} $rhsTranslated"
        }
        (setup, toC)
      case fred.MatchExpr(_, _, _)    => ???
      case fred.FnCall(_, _, _, _, _) => ???
      case letExpr @ fred.LetExpr(name, value, body, _) =>
        val (valueSetup, valueToC) = exprToC(value)
        val typ = typer.types(expr)
        val setup = s"""|$valueSetup
                        |${typeRefToC(
                         typ.name
                       )} ${name.value} = $valueToC;""".stripMargin
        val (bodySetup, bodyToC) = exprToC(body)(using
          bindings.withVar(name.value, VarDef.Let(letExpr, typ))
        )
        (s"$setup\n$bodySetup", bodyToC)
      case fred.IfExpr(cond, thenBody, elseBody, _) =>
        val (condSetup, condC) = exprToC(cond)
        val (thenSetup, thenC) = exprToC(thenBody)
        val (elseSetup, elseC) = exprToC(elseBody)

        val typ = typeRefToC(typer.types(expr).name)
        val resVar = newVar(expr.hashCode())

        val setup = s"""|$condSetup
                        |int $resVar;
                        |if ($condC) {
                        |${indent(1)(thenSetup)}
                        |  $resVar = $thenC;
                        |} else {
                        |${indent(1)(elseSetup)}
                        |  $resVar = $elseC;
                        |}""".stripMargin

        (setup, resVar)
      case fred.FieldAccess(_, _, _) => ???
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
      case name  => s"$name*"
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
