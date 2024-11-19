package fred

import org.scalatest.funsuite.AnyFunSuite
import snapshot4s.scalatest.SnapshotAssertions
import snapshot4s.generated.snapshotConfig

class TranslatorTests extends AnyFunSuite with SnapshotAssertions {
  test("Generating a type") {
    val parsed = Parser.parse("""
      data Foo
        = Bar {
            mut foo: Foo,
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

  test("Setting fields") {
    val parsed = Parser.parse("""
      data Foo = Foo { field: int }
      fn bar(f: Foo): int =
        set f.field 5;
        set f.field 6
      """)
    given Typer = Typer.resolveAllTypes(parsed)
    val generated = Translator.toC(parsed)
    assertFileSnapshot(generated.toString, "gen/set-field-sfdu29.c")
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

  test("Types in good SCCs shouldn't be marked as PCRs") {
    // There's no possibility of cycles here, because all references are immutable
    val parsed = Parser.parse("""
      data Rec = Rec { rec: Rec }
      """)
    given Typer = Typer.resolveAllTypes(parsed)
    val generated = Translator.toC(parsed)
    assertFileSnapshot(generated.toString, "gen/good-scc-jifs893.c")
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
