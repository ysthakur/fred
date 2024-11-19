package fred

import scala.sys.process.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object Compiler {
  private val RuntimeHeader = "runtime/runtime.h"

  def compile(code: String, outExe: String): Unit = {
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
    val generatedC = Translator.toC(parsedFile)
    invokeGCC(generatedC, outExe)
  }

  def invokeGCC(generated: String, outExe: String): Unit = {
    val io = ProcessIO(
      in => {
        in.write(generated.getBytes())
        in.close()
      },
      out => {
        print(String(out.readAllBytes()))
      },
      err => {
        System.err.print(String(err.readAllBytes()))
      }
    )

    assert(
      s"gcc -I ${includesFolder()} -o $outExe -x c -".run(io).exitValue() == 0
    )
  }

  /** Path to the folder with header files to include */
  private def includesFolder(): String = {
    val runtimeFile =
      this.getClass().getClassLoader().getResource(RuntimeHeader).toURI()
    Paths.get(runtimeFile).getParent().toString()
  }
}
