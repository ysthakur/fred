package fred

import snapshot4s.munit.SnapshotAssertions
import snapshot4s.generated.snapshotConfig

class TranslatorTests extends munit.FunSuite with SnapshotAssertions {
  test("Generating a type") {
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
}
