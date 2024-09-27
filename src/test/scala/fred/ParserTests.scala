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
    assertFileSnapshot(parsed.toString, "parse-type-k7bu65.scala")
  }

  test("Parsing expressions") {
    val parsed = Parser.parse("""
      fn foo(): int {
        let x = 3;
        3
      }
      """)
    assertFileSnapshot(parsed.toString, "parse-expr-q8p3hfe.scala")
  }
}
