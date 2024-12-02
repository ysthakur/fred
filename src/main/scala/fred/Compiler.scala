package fred

import scala.sys.process.*
import java.nio.file.Paths
import java.io.File

import scopt.OParser

object Compiler {
  private val RuntimeHeader = "runtime/runtime.h"

  case class Settings(
      translatorSettings: Translator.Settings = Translator.Settings()
  )

  def main(args: Array[String]): Unit = {
    case class Options(
        inFile: Option[File] = None,
        outExe: Option[String] = None,
        rcAlgo: Translator.RcAlgo = Translator.RcAlgo.Mine
    )

    val builder = OParser.builder[Options]
    val parser = {
      import builder._
      OParser.sequence(
        programName("fred"),
        arg[File]("file").action((f, opts) => opts.copy(inFile = Some(f)))
          .text("Input file"),
        opt[String]('o', "out").action((f, opts) => opts.copy(outExe = Some(f)))
          .text("Output executable"),
        opt[Unit]("lazy-mark-scan-only").action((_, opts) =>
          opts.copy(rcAlgo = Translator.RcAlgo.LazyMarkScan)
        ).text("Use base lazy mark scan algorithm instead of my cool one :(")
      )
    }
    OParser.parse(parser, args, Options()) match {
      case Some(opts) =>
        val codeSource = io.Source.fromFile(opts.inFile.get)
        val code =
          try { codeSource.mkString }
          finally { codeSource.close() }
        Compiler.compile(
          code,
          opts.outExe.get,
          settings =
            Settings(translatorSettings = Translator.Settings(opts.rcAlgo))
        )
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
      try { Typer.resolveAllTypes(parsedFile) }
      catch {
        case CompileError(msg, span) =>
          println(s"Error at $span: $msg")
          println(code.substring(span.start, span.end))
          System.exit(1)
          throw new AssertionError("Shouldn't get here")
      }
    val generatedC = Translator
      .toC(parsedFile, settings = settings.translatorSettings)
    invokeGCC(generatedC, outExe)
  }

  def invokeGCC(generated: String, outExe: String): Unit = {
    val io = ProcessIO(
      in => {
        in.write(generated.getBytes())
        in.close()
      },
      out => { print(String(out.readAllBytes())) },
      err => { System.err.print(String(err.readAllBytes())) }
    )

    assert(
      s"gcc -I ${includesFolder()} -o $outExe -x c -".run(io).exitValue() == 0
    )
  }

  /** Path to the folder with header files to include */
  private def includesFolder(): String = {
    val runtimeFile = this.getClass().getClassLoader()
      .getResource(RuntimeHeader).toURI()
    Paths.get(runtimeFile).getParent().toString()
  }
}
