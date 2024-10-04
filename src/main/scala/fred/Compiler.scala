package fred

object Compiler {
  def compile(code: String): String = {
    val parsedFile = Parser.parse(code)
    given typer: Typer =
      try {
        Typer.resolveAllTypes(parsedFile)
      } catch {
        case CompileError(msg, span) =>
          println(s"Error at $span: $msg")
          println(code.substring(span.start, span.end))
          System.exit(1)
          throw new AssertionError("Shouldn't get here")
      }
    Translator.toC(parsedFile)
  }
}
