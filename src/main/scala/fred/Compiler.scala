package fred

import scala.sys.process.*
import java.nio.file.Files
import java.nio.file.Path

object Compiler {
  def compile(code: String, outC: Path, outExe: String): Unit = {
    val parsedFile = Parser.parse(code)
    given typer: Typer =
      try {
        Typer.resolveAllTypes(parsedFile)
      } catch {
        case CompileError(msg, span) =>
          println(s"Error at $span: $msg")
          println(code.substring(span.start, span.end))
          // System.exit(1)
          throw new AssertionError("Shouldn't get here")
      }
    val generatedC = Translator.toC(parsedFile)
    Files.write(outC, generatedC.getBytes())
    println(s"gcc ${outC.toAbsolutePath()}".!!)
  }
}
