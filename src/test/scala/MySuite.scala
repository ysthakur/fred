import snapshot4s.munit.SnapshotAssertions
import snapshot4s.generated.snapshotConfig

class MySuite extends munit.FunSuite with SnapshotAssertions {
  test("snapshot4s can fill in the blanks") {
    val mySnapshotWorkflow = "snapshot4s"
    assertInlineSnapshot(mySnapshotWorkflow, "snapshot4s")
  }
  test("snapshot4s can update code") {
    val mySnapshotCode = List(1, 2, 3)
    assertInlineSnapshot(mySnapshotCode, List(1, 2, 3))
  }
  test("snapshot4s can work with files") {
    val mySnapshotWorkflow = "snapshot4s"
    assertFileSnapshot(mySnapshotWorkflow, "mySnapshotWorkflow")
  }
}
