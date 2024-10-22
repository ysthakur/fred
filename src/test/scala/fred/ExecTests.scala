package fred

package fred

import snapshot4s.munit.SnapshotAssertions
import snapshot4s.generated.snapshotConfig

import scala.sys.process.*
import java.nio.file.Paths

class ExecTests extends munit.FunSuite with SnapshotAssertions {
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
    val compiled = Compiler.compile(code, Paths.get("foo.c"), "a.out")

    val valgrindOutBuf = StringBuilder()
    assertNoDiff(
      "7",
      "valgrind -s --leak-check=yes ./a.out" !! ProcessLogger(
        _ => {},
        err => valgrindOutBuf.append('\n').append(err)
      )
    )
    val valgrindOut = valgrindOutBuf.toString
    assert(valgrindOut.contains("ERROR SUMMARY: 0 errors"), valgrindOut)
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
    val compiled = Compiler.compile(code, Paths.get("foo.c"), "a.out")

    val valgrindOutBuf = StringBuilder()
    assertNoDiff(
      "3",
      "valgrind -s --leak-check=yes ./a.out" !! ProcessLogger(
        _ => {},
        err => valgrindOutBuf.append('\n').append(err)
      )
    )
    val valgrindOut = valgrindOutBuf.toString
    assert(valgrindOut.contains("ERROR SUMMARY: 0 errors"), valgrindOut)
  }

  test("Contrived example") {
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

fn addFile(ctx: Context, file: File): int =
  set ctx.files FileCons { ctx: ctx, head: file, tail: ctx.files };
  0
fn addExpr(file: File, expr: Expr): int =
  set file.exprs ExprCons { head: expr, tail: file.exprs };
  0

fn main(): int =
  let ctx = CtxRef { ref: Context { name: "foo", files: FileNil {} } } in
  let file = File { exprs: ExprNil {} } in
  addFile(ctx.ref, file);
  (let expr = Expr { file: file } in
  addExpr(file, expr);
  set ctx.ref Context { name: "other context", files: FileNil {} };
  0)
      """
    val compiled = Compiler.compile(code, Paths.get("foo.c"), "a.out")

    val valgrindOutBuf = StringBuilder()
    assertNoDiff(
      "",
      "valgrind -s --leak-check=yes ./a.out" !! ProcessLogger(
        _ => {},
        err => valgrindOutBuf.append('\n').append(err)
      )
    )
    val valgrindOut = valgrindOutBuf.toString
    assert(valgrindOut.contains("ERROR SUMMARY: 0 errors"), valgrindOut)
  }
}
