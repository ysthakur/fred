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
      .map(field => s"${typeRefToC(field.typ)} ${field.name.value};")
      .mkString(" ")
    s"struct { $fields };"
  }

  private def typeRefToC(typeRef: TypeRef) = {
    typeRef.name match {
      case "int" => "int"
      case "str" => "char*"
      case name  => s"$name*"
    }
  }

  /** Mangle a constructor name to use it as the tag for a tagged union
    * representing the original ADT
    */
  private def tagName(ctorName: String): String = s"${ctorName}_tag"

  private def fnToC(fn: FnDef) = {}
}
