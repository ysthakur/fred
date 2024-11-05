package fred

import snapshot4s.munit.SnapshotAssertions
import snapshot4s.generated.snapshotConfig

class SCCTests extends munit.FunSuite with SnapshotAssertions {
  def createFile(graph: Map[String, Set[String]]): ParsedFile = {
    ParsedFile(
      graph.map { (name, neighbors) =>
        val spannedName = Spanned(name, Span.synthetic)
        val fields = graph(name).toList.map { neighborName =>
          FieldDef(
            false,
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

  def simplifySCCs(sccs: List[Set[TypeDef]]): List[Set[String]] = {
    sccs.map(_.map(_.name))
  }

  test("Basic SCCs") {
    val file = createFile(
      Map(
        "A" -> Set("B", "C"),
        "B" -> Set("A"),
        "C" -> Set("D"),
        "D" -> Set("E"),
        "E" -> Set("F"),
        "F" -> Set("D")
      )
    )

    assertFileSnapshot(
      pprint.apply(simplifySCCs(SCC.findSCCs(file))).plainText,
      "scc/basic-fjw38es.scala"
    )
  }
}
