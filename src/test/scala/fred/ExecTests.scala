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
        let list = Cons { value: 1, next: Cons { value: 2, next: Cons { value: 3, next: Nil { } } } } in
        printf("%d\n", sum(list));
        0
      """
    val compiled = Compiler.compile(code, Paths.get("foo.c"), "a.out")
    assertNoDiff("6", "./a.out".!!)
  }
}
