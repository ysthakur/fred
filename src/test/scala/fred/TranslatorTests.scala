package fred

import snapshot4s.munit.SnapshotAssertions
import snapshot4s.generated.snapshotConfig

class TranslatorTests extends munit.FunSuite with SnapshotAssertions {
  test("Generating a type") {
    val parsed = Parser.parse("""
      data Foo
        = Bar {
            mut foo: Foo,
            fred: Fred,
            common: str,
            notcommon: str
          }
        | Baz {
            blech: str,
            mut gah: int,
            common: str,
            notcommon: int
          }
      
      fn foo(param: Foo): str = param.common
      """)
    given Typer = Typer.resolveAllTypes(parsed)
    val generated = Translator.toC(parsed)
    assertFileSnapshot(generated.toString, "gen/type-aslid7fy.c")
  }

  test("Sequence operator") {
    val parsed = Parser.parse("""
      fn foo(bar: int): int =
        bar;
        3
      """)
    given Typer = Typer.resolveAllTypes(parsed)
    val generated = Translator.toC(parsed)
    assertFileSnapshot(generated.toString, "gen/seq-as8yfe.c")
  }

  test("If expressions") {
    val parsed = Parser.parse("""
      fn foo(bar: int): str =
        if (let g = 3 in g) then
          let y = 5 in
          "foo"
        else
          let y = "foo" in
          y
      """)
    given Typer = Typer.resolveAllTypes(parsed)
    val generated = Translator.toC(parsed)
    assertFileSnapshot(generated.toString, "gen/if-3j4itr.c")
  }

  test("Function calls") {
    val parsed = Parser.parse("""
      fn fact(i: int): int =
        if i == 0 then 1
        else i * fact(i - 1)
      """)
    given Typer = Typer.resolveAllTypes(parsed)
    val generated = Translator.toC(parsed)
    assertFileSnapshot(generated.toString, "gen/fn-call-83jief.c")
  }

  test("Complex test") {
    val parsed = Parser.parse("""
      data Foo
        = Bar {
            x: int,
            y: int
          }
        | Baz {
            a: str,
            b: int
          }
      
      fn foo(foo: Foo): int =
        foo match {
          Bar { x: x, y: yyy } => x + yyy,
          Baz { a: astr, b: b } => b
        }
      
      fn main(): int =
        let foo = Bar { x: 1, y: 2 } in
        foo(foo)
      """)
    given Typer = Typer.resolveAllTypes(parsed)
    val generated = Translator.toC(parsed)
    assertFileSnapshot(generated.toString, "gen/complex-liudr567.c")
  }
}
