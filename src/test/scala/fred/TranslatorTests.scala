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
            blech: Bar,
            mut gah: int
          }
      """)
    val generated = Translator.toC(parsed)
    assertFileSnapshot(generated.toString, "gen/type-aslid7fy.c")
  }
}
