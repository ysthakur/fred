package fred

import snapshot4s.munit.SnapshotAssertions
import snapshot4s.generated.snapshotConfig

class SCCTests extends munit.FunSuite with SnapshotAssertions {

  /** This is just a helper to avoid typing out calls to the TypeDef ctor */
  def createFile(graph: Map[String, Set[(Boolean, String)]]): ParsedFile = {
    ParsedFile(
      graph.map { (name, neighbors) =>
        val spannedName = Spanned(name, Span.synthetic)
        val fields = graph(name).toList.map { (mutable, neighborName) =>
          FieldDef(
            mutable,
            Spanned(s"field$neighborName", Span.synthetic),
            TypeRef(neighborName, Span.synthetic),
            Span.synthetic
          )
        }
        TypeDef(
          spannedName,
          List(
            EnumCase(spannedName, fields, Span.synthetic)
          ),
          Span.synthetic
        )
      }.toList,
      Nil
    )
  }

  def createFileImmutable(graph: Map[String, Set[String]]): ParsedFile = {
    createFile(graph.mapValues(_.map(false -> _)).toMap)
  }

  def simplifySCCs(sccs: List[Set[TypeDef]]): List[Set[String]] = {
    sccs.map(_.map(_.name))
  }

  test("Basic SCCs") {
    val file = createFileImmutable(
      Map(
        "A" -> Set("B", "C"),
        "B" -> Set("A"),
        "C" -> Set("D"),
        "D" -> Set("E"),
        "E" -> Set("F"),
        "F" -> Set("D")
      )
    )

    assertEquals(
      List(Set("B", "A"), Set("C"), Set("D", "F", "E")),
      simplifySCCs(Cycles.findSCCs(file))
    )
  }

  test("Multiple SCCs at same level") {
    val file = createFileImmutable(
      Map(
        "A" -> Set("B", "C", "D", "E"),
        "B" -> Set(),
        "C" -> Set("G"),
        "D" -> Set("G"),
        "E" -> Set("F"),
        "F" -> Set("E"),
        "G" -> Set()
      )
    )

    assertEquals(
      List(Set("A"), Set("D"), Set("C"), Set("G"), Set("B"), Set("F", "E")),
      simplifySCCs(Cycles.findSCCs(file))
    )
  }

  test("Is basic bad SCC detected") {
    val file = createFile(
      Map(
        "A" -> Set((true, "B")),
        "B" -> Set((false, "C")),
        "C" -> Set((false, "A"))
      )
    )

    val sccs = Cycles.findSCCs(file)
    assertEquals(sccs.size, 1)
    assertEquals(Cycles.badSCCs(sccs, file), List(0))
  }

  test("Is basic good SCC not incorrectly marked bad") {
    val file = createFileImmutable(
      Map(
        "A" -> Set("B", "C", "D", "E"),
        "B" -> Set(),
        "C" -> Set("G"),
        "D" -> Set("G"),
        "E" -> Set("F"),
        "F" -> Set("E"),
        "G" -> Set("D")
      )
    )

    val sccs = Cycles.findSCCs(file)
    assert(Cycles.badSCCs(sccs, file).isEmpty)
  }
}
