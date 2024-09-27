package fred

object Compiler {
  def compile(code: String) = {
    val parsed = Parser.parse(code)
  }
}
