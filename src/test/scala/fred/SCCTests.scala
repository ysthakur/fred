package fred

import org.scalatest.funsuite.AnyFunSuite
import snapshot4s.scalatest.SnapshotAssertions
import snapshot4s.generated.snapshotConfig

class SCCTests extends AnyFunSuite with SnapshotAssertions {

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

    assert(
      List(Set("B", "A"), Set("C"), Set("D", "F", "E"))
        === simplifySCCs(Cycles.fromFile(file).sccs)
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

    assert(
      List(
        Set("A"),
        Set("D"),
        Set("C"),
        Set("G"),
        Set("B"),
        Set("F", "E")
      ) === simplifySCCs(Cycles.fromFile(file).sccs)
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

    val cycles = Cycles.fromFile(file)
    assert(cycles.sccs.size === 1)
    assert(cycles.badSCCs === List(0))
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

    val cycles = Cycles.fromFile(file)
    assert(cycles.badSCCs.isEmpty)
  }
}
