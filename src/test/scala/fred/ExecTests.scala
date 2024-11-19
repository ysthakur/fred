package fred

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import snapshot4s.scalatest.SnapshotAssertions
import snapshot4s.generated.snapshotConfig

import scala.sys.process.*
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.Path

class ExecTests
    extends AnyFunSuite,
      SnapshotAssertions,
      ScalaCheckPropertyChecks {
  def valgrindCheck(code: String, outFile: String)(expected: String): Unit = {
    valgrindCheck(Parser.parse(code), outFile, Some(expected), snapshot = true)
  }

  def valgrindCheck(
      parsedFile: ParsedFile,
      outFile: String,
      expected: Option[String],
      snapshot: Boolean
  ): Unit = {
    given typer: Typer = Typer.resolveAllTypes(parsedFile)
    val generatedC = Translator.toC(parsedFile)

    if (snapshot) {
      assertFileSnapshot(generatedC, s"exec/$outFile")
      s"src/test/resources/snapshot/exec/$outFile"
    }

    Compiler.invokeGCC(generatedC, "a.out")

    val stderrBuf = StringBuilder()
    val stdout =
      try {
        "valgrind --leak-check=full --show-leak-kinds=all -s ./a.out" !! ProcessLogger(
          _ => {},
          err => stderrBuf.append('\n').append(err)
        )
      } catch {
        case e: RuntimeException =>
          throw RuntimeException(stderrBuf.toString, e)
      }
    val valgrindOut = stderrBuf.toString
    expected match {
      case Some(expected) =>
        assert(stdout.trim() === expected.trim(), valgrindOut)
      case None =>
    }
    assert(valgrindOut.contains("ERROR SUMMARY: 0 errors"), valgrindOut)
  }

  test("Basic main function") {
    val code = """
      data List
        = Nil { }
        | Cons {
            value: int,
            next: List
          }
      
      fn sum(list: List): int =
        list match {
          Nil { } => 0,
          Cons { value: value, next: tail } => value + sum(tail)
        }

      fn main(): int =
        let list = Cons { value: 1, next: Cons { value: 2, next: Cons { value: 4, next: Nil { } } } } in
        printf("%d\n", sum(list));
        0
      """

    valgrindCheck(code, "basic-main.c")("7")
  }

  test("Basic cycle") {
    val code = """
      data Option
        = None {}
        | Some { value: List }
      data List = List { value: int, mut next: Option }

      fn main(): int =
        let a = List { value: 1, next: None {} } in
        let b = List { value: 2, next: Some { value: a } } in
        set a.next Some { value: b };
        printf("%d\n", a.value + b.value);
        0
      """

    valgrindCheck(code, "basic-cycle.c")("3")
  }

  test("Needs sorting") {
    // This test case needs the objects to be sorted by SCC. Otherwise, the quadratic
    // scanning problem will show up

    val code = """
      data CtxRef = CtxRef {
        ref: Context
      }

      data Context = Context {
        name: str,
        mut files: FileList
      }

      data FileList
        = FileNil {}
        | FileCons {
          ctx: Context,
          head: File,
          tail: FileList
        }

      data File = File {
        mut exprs: ExprList
      }

      data ExprList
        = ExprNil {}
        | ExprCons {
          head: Expr,
          tail: ExprList
        }

      data Expr = Expr {
        file: File
      }

      fn main(): int =
        let ctx = CtxRef { ref: Context { name: "foo", files: FileNil {} } } in
        (let file = File { exprs: ExprNil {} } in
          let actualCtx = ctx.ref in
          set actualCtx.files FileCons { ctx: ctx.ref, head: file, tail: ctx.ref.files };
          (let expr = Expr { file: file } in
            set file.exprs ExprCons { head: expr, tail: file.exprs });
        set ctx.ref Context { name: "other context", files: FileNil {} };
        0)
      """

    valgrindCheck(code, "contrived-needs-sorting.c")("")
  }

  test("Simple generated programs") {
    given PropertyCheckConfiguration =
      PropertyCheckConfiguration(minSize = 1, sizeRange = 10)
    forAll(GenerateTypes.genTypesAux().flatMap(GenerateTypes.genCode)) {
      parsedFile => valgrindCheck(parsedFile, "foo.c", None, snapshot = false)
    }
  }
}
