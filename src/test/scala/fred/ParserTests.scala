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
      "parser/type-k7bu65.scala"
    )
  }

  test("Parsing basic expressions") {
    val parsed = Parser.parse("""
      fn foo(param: ParamType, bleh: int): int =
        let x = 3 in
        foo(bleh, 4 + x);
        x
      """)
    assertFileSnapshot(
      pprint.apply(parsed).plainText,
      "parser/basic-expr-q8p3hfe.scala"
    )
  }

  test("Parsing if expressions") {
    val parsed = Parser.parse("""
      fn foo(): int =
        if foo then
          if bar then x else y
        else
          if 3 + 2 then
            bleh();
            5
          else
            foo();
            bar
      """)
    assertFileSnapshot(
      pprint.apply(parsed).plainText,
      "parser/if-expr-asp8eyof7.scala"
    )
  }

  test("Precedence of if and match") {
    val parsed = Parser.parse("""
      fn foo(): int =
        if 1 then 2
        else
          3 match {
          }
      """)
    assertFileSnapshot(
      pprint.apply(parsed).plainText,
      "parser/match-and-if-expr-aw83lasd.scala"
    )
  }

  test("Parsing match expressions") {
    val parsed = Parser.parse("""
      fn foo(): int =
        foo + 2 match {
          Foo { a: b, c: d } => 7 + 2,
          Bar { } => empty,
          Blech { a: b } => single(thing)
        } match {
          ChainedMatch { } => just(because)
        }
      """)
    assertFileSnapshot(
      pprint.apply(parsed).plainText,
      "parser/match-expr-qpw8jf.scala"
    )
  }

  test("Setting fields") {
    val parsed = Parser.parse("""|fn foo(): int =
                                 |set foo.bar 4;
                                 |foo""".stripMargin)
    assertFileSnapshot(
      pprint.apply(parsed).plainText,
      "parser/set-d8ulsb.scala"
    )
  }
}
