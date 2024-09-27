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
    val tagNames = typ.cases.map(enumCase => tagName(enumCase.name.value)).mkString(", ")
    val tagEnum = s"enum ${name}_kind { ${tagNames} }"
    val struct = s"""|struct ${name} {
                     |  enum ${name}_kind ${KindField};
                     |  union {
                     |  };
                     |}""".stripMargin
    tagEnum + "\n" + struct
  }

  /** Mangle a constructor name to use it as the tag for a tagged union
    * representing the original ADT
    */
  private def tagName(ctorName: String): String = s"${ctorName}_tag"

  private def fnToC(fn: FnDef) = {}
}
