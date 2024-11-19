package fred

import scala.sys.process.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object Compiler {
  private val RuntimeHeader = "runtime/runtime.h"

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
    println(s"gcc -I ${includesFolder()} ${outC.toAbsolutePath()}".!!)
  }

  /** Path to the folder with header files to include */
  def includesFolder(): String = {
    val runtimeFile =
      this.getClass().getClassLoader().getResource(RuntimeHeader).toURI()
    Paths.get(runtimeFile).getParent().toString()
  }
}
