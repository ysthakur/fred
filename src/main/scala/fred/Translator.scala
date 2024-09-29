package fred

object Translator {
  private val KindField = "kind"

  def toC(file: ParsedFile): String = {
    file.typeDefs.map(typeToC).mkString("\n") + "\n" + file.fns
      .map(fnToC)
      .mkString("\n")
  }

  private def typeToC(typ: TypeDef) = {
    val name = typ.name.value
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
        s"${typeRefToC(field.typ)} ${mangledFieldName(enumCase, field.name.value)};"
      }
      .mkString(" ")
    s"struct { $fields };"
  }

  private def fnToC(fn: FnDef) = {
    val params = fn.params
      .map(param => s"${typeRefToC(param.typ)} ${param.name.value}")
      .mkString(", ")
    s"""|${typeRefToC(fn.returnType)} ${fn.name.value} ($params) {
        |${indent(2)(exprToC(fn.body)._2)}
        |}""".stripMargin
  }

  /** @return
    *   A tuple containing the C code for setup and the C code for the actual
    *   expression. The first string is necessary for let expressions, match
    *   expressions, and the like, where in C, you need some statements to
    *   define a variable before you can use it in an expression
    */
  private def exprToC(expr: Expr): (List[String], String) = {
    expr match {
      case IntLiteral(value, _) => (Nil, value.toString)
      case StringLiteral(value, _) =>
        (Nil, s"\"${value.replace("\"", "\\\"")}\"")
      case VarRef(name, _, _)                            => (Nil, name)
      case BinExpr(lhs, op, rhs, typ) if op == BinOp.Seq => ???
      case BinExpr(lhs, op, rhs, typ) =>
        val (lhsSetup, lhsTranslated) = exprToC(lhs)
        val (rhsSetup, rhsTranslated) = exprToC(rhs)
        (
          lhsSetup ++ rhsSetup,
          s"$lhsTranslated ${op.value.text} $rhsTranslated"
        )
    }
  }

  private def typeRefToC(typeRef: TypeRef) = {
    typeRef.name match {
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
    s.split("\n").map(line => "  " * level + level).mkString("\n")
  }
}
