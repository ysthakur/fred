package fred

import scala.sys.process.*
import java.nio.file.Files
import java.nio.file.Path
import scala.util.Random

import org.scalatest.funsuite.AnyFunSuite
import snapshot4s.scalatest.SnapshotAssertions
import snapshot4s.generated.snapshotConfig

import fred.Compiler.Settings

class ExecTests extends AnyFunSuite, SnapshotAssertions {
  def valgrindCheck(
      code: String,
      outFile: String,
      settings: Settings = Settings()
  )(expected: String): Unit = {
    ExecTests.valgrindCheck(
      Parser.parse(code),
      Some(assertFileSnapshot(_, s"exec/$outFile")),
      Some(expected),
      settings = settings
    )
  }

  test("Immediately dropped object") {
    val code = """
      data Foo = Foo { }

      fn main(): int =
        Foo {};
        0
      """

    valgrindCheck(code, "immediate-drop.c")("")
  }

  test("Unused variables") {
    val code = """
    data List = Cons { next: List } | Nil {}

    fn f(list: List): int =
      list match {
        Cons { next: list } =>
          let list = Nil {} in
          0,
        Nil {} => 2
      }

    fn main(): int =
      f(Cons { next: Nil {} });
      0
    """
    valgrindCheck(code, "unused-3jpowi.c")("")
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

  test("Lazy mark scan only") {
    // These types should all be lumped into the same SCC
    val code = """
      data FooList
        = FooCons {
            foo: Foo,
            tail: FooList
          }
        | FooNil {}
      data Foo = Foo { bar: Bar }
      data Bar = Bar {}

      fn main(): int =
        let bar = Bar {} in
        let foo = Foo { bar: bar } in
        let foos = FooCons { foo: foo, tail: FooNil {} } in
        0
      """

    valgrindCheck(
      code,
      "lazy-mark-scan-83wesh.c",
      settings = Settings(rcAlgo = Compiler.RcAlgo.LazyMarkScan)
    )("")
  }

  test("PCR bucket emptied and refilled") {
    // The bucket for SCC 0 (the only SCC) should be deleted after the call to
    // processAllPCRs, then created again
    val code = """
      data Foo = Foo {
        mut self: OptFoo
      }
      data OptFoo
        = SomeFoo { value: Foo }
        | NoneFoo {}

      fn main(): int =
        let v0 = Foo { self: NoneFoo {} } in
        let v1 = Foo { self: NoneFoo {} } in
        set v0.self SomeFoo { value: v1 };
        set v0.self SomeFoo { value: v0 };
        c("processAllPCRs();");
        set v0.self SomeFoo { value: v1 };
        0
      """
    valgrindCheck(code, "bucket-empty-recreate-3jilws.c")("")
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
}

object ExecTests {
  def valgrindCheck(
      parsedFile: ParsedFile,
      snapshot: Option[String => Unit],
      expected: Option[String],
      save: Option[String] = None,
      settings: Settings = Settings(),
      processC: String => String = c => c
  ): Unit = {
    given typer: Typer = Typer.resolveAllTypes(parsedFile)
    val generatedC = processC(Translator.toC(parsedFile, settings = settings))

    snapshot match {
      case Some(assertSnapshot) => assertSnapshot(generatedC)
      case None                 =>
    }

    save match {
      case Some(savePath) => Files
          .write(Path.of(savePath), generatedC.getBytes())
      case None =>
    }

    val exeName = Random.alphanumeric.take(10).mkString + ".bin"
    Compiler.invokeGCC(generatedC, exeName, settings)

    val stderrBuf = StringBuilder()
    val stdout =
      try {
        s"valgrind --leak-check=full --show-leak-kinds=all -s ./$exeName" !!
          ProcessLogger(_ => {}, err => stderrBuf.append('\n').append(err))
      } catch {
        case e: RuntimeException =>
          throw RuntimeException(stderrBuf.toString, e)
      } finally { Files.deleteIfExists(Path.of(exeName)) }
    val valgrindOut = stderrBuf.toString
    expected match {
      case Some(expected) => assert(
          stdout.trim() == expected.trim(),
          s"Stdout: $stdout\n---\nStderr: $valgrindOut"
        )
      case None =>
    }
    assert(valgrindOut.contains("ERROR SUMMARY: 0 errors"), valgrindOut)
  }
}
