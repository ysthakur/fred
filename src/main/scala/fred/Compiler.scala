package fred

import scala.sys.process.*
import java.io.File

import scopt.OParser
import java.nio.file.Files
import java.nio.file.Path

object Compiler {
  private val RuntimeHeader = "runtime/runtime.h"

  case class Settings(
      rcAlgo: RcAlgo = RcAlgo.Mine,
      includeMemcheck: Boolean = false
  )

  enum RcAlgo {
    case LazyMarkScan, Mine
  }

  def main(args: Array[String]): Unit = {
    case class Options(
        inFile: Option[File] = None,
        outExe: String = "a.out",
        settings: Settings = Settings()
    )

    val builder = OParser.builder[Options]
    val parser = {
      import builder._
      OParser.sequence(
        programName("fred"),
        arg[File]("file").action((f, opts) => opts.copy(inFile = Some(f)))
          .text("Input file"),
        opt[String]('o', "out").action((f, opts) => opts.copy(outExe = f))
          .text("Output executable (default: a.out)"),
        opt[Unit]("lazy-mark-scan-only").action((_, opts) =>
          opts.copy(settings = opts.settings.copy(rcAlgo = RcAlgo.LazyMarkScan))
        ).text("Use base lazy mark scan algorithm instead of my cool one :(")
      )
    }
    OParser.parse(parser, args, Options()) match {
      case Some(opts) =>
        val codeSource = io.Source.fromFile(opts.inFile.get)
        val code =
          try codeSource.mkString
          finally codeSource.close()
        Compiler.compile(code, opts.outExe, settings = opts.settings)
      case None =>
    }
  }

  def compile(
      code: String,
      outExe: String,
      settings: Settings = Settings()
  ): Unit = {
    val parsedFile = Parser.parse(code)
    given typer: Typer =
      try Typer.resolveAllTypes(parsedFile)
      catch {
        case CompileError(msg, span) =>
          println(s"Error at $span: $msg")
          println(code.substring(span.start, span.end))
          System.exit(1)
          throw new AssertionError("Shouldn't get here")
      }
    val generatedC = Translator.toC(parsedFile, settings = settings)
    Files.write(Path.of("out.c"), generatedC.getBytes())
    invokeGCC(generatedC, outExe, settings)
  }

  def invokeGCC(generated: String, outExe: String, settings: Settings): Unit = {
    val runtimeHeader = this.getClass().getClassLoader()
      .getResourceAsStream(RuntimeHeader)
    val io = ProcessIO(
      in => {
        in.write(runtimeHeader.readAllBytes())
        // TODO removing #include runtime.h is a horrendous hack
        in.write(generated.replaceAll("#include \"runtime.h\"", "").getBytes())
        in.close()
      },
      out => print(String(out.readAllBytes())),
      err => System.err.print(String(err.readAllBytes()))
    )

    val extraIncludes =
      if (settings.includeMemcheck) "-I /usr/include/valgrind" else ""

    assert(s"gcc $extraIncludes -o $outExe -x c -".run(io).exitValue() == 0)
    File(outExe).setExecutable(true)

    runtimeHeader.close()
  }
}
