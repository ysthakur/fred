package fred

import snapshot4s.munit.SnapshotAssertions
import snapshot4s.generated.snapshotConfig

class ParserTests extends munit.FunSuite with SnapshotAssertions {
  test("Parsing a type") {
    val parsed = Parser.parse("""
      data Foo
        = Bar {
            mut foo: Foo,
            fred: Fred
          }
        | Baz {
            blech: Bar,
            mut gah: int
          }
      """)
    assertFileSnapshot(
      pprint.apply(parsed).plainText,
      "parse-type-k7bu65.scala"
    )
  }

  test("Parsing basic expressions") {
    val parsed = Parser.parse("""
      fn foo(param: ParamType, bleh: int): int {
        let x = 3 in
        foo(bleh, 4 + x);
        x
      }
      """)
    assertFileSnapshot(
      pprint.apply(parsed).plainText,
      "parse-expr-q8p3hfe.scala"
    )
  }

  test("Parsing if expressions") {
    val parsed = Parser.parse("""
      fn foo(): int {
        if foo then
          if bar then x else y
        else
          if 3 + 2 then
            bleh();
            5
          else
            foo();
            bar
      }
      """)
    assertFileSnapshot(
      pprint.apply(parsed).plainText,
      "parse-if-expr-asp8eyof7.scala"
    )
  }
}
