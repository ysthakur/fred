package fred

import snapshot4s.munit.SnapshotAssertions
import snapshot4s.generated.snapshotConfig

class TyperTests extends munit.FunSuite with SnapshotAssertions {
  test("Typing a thing") {
    val parsed = Parser.parse("""
      data Foo
        = Bar {
            mut foo: Foo,
            fred: Fred
          }
        | Baz {
            blech: str,
            mut gah: int
          }
      
      fn foo(param: Foo): int =
        let x = 2 in
        param match {
          Bar { foo: blech } => blech,
          Baz { blech: blech } => param
        };
        x
      """)
    val typer = Typer.resolveAllTypes(parsed)
    assertFileSnapshot(
      pprint
        .apply(typer.types.toList.sortBy { case (expr, _) =>
          (expr.span.start, expr.span.end, expr.hashCode())
        })
        .plainText,
      "typer/aslid7fy.scala"
    )
  }

  test("Ensuring function body matches return type") {
    val parsed = Parser.parse("""
      fn foo(): int = "foobar"
      """)
    intercept[CompileError] {
      Typer.resolveAllTypes(parsed)
    }
  }

  test("Ensuring all branches of a match expression are the same") {
    val parsed = Parser.parse("""
      data Foo
        = Bar {
            mut foo: Foo,
            fred: Fred
          }
        | Baz {
            blech: str,
            mut gah: int
          }
      
      fn foo(param: Foo): int =
        param match {
          Bar { foo: blech } => 2,
          Baz { blech: blech } => blech
        }
      """)
    intercept[CompileError] {
      Typer.resolveAllTypes(parsed)
    }
  }

  test("Same variable name, separate scopes") {
    val parsed = Parser.parse("""
      fn foo(): int =
        if 0 then
          let x = 2 in x
        else
          let x = "foo" in
          let y = x in
          4
      """)
    val typer = Typer.resolveAllTypes(parsed)
    assertFileSnapshot(
      pprint
        .apply(typer.types.toList.sortBy { case (expr, _) =>
          (expr.span.start, expr.span.end, expr.hashCode())
        })
        .plainText,
      "typer/scope-li8ae4.scala"
    )
  }
}
