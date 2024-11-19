package fred

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import snapshot4s.scalatest.SnapshotAssertions
import snapshot4s.generated.snapshotConfig

class SCCTests
    extends AnyFunSuite,
      SnapshotAssertions,
      ScalaCheckPropertyChecks {

  /** This is just a helper to avoid typing out calls to the TypeDef ctor */
  def createFile(graph: Map[String, Set[(Boolean, String)]]): ParsedFile = {
    ParsedFile(
      graph.map { (name, neighbors) =>
        val spannedName = Spanned(name, Span.synth)
        val fields = graph(name).toList.map { (mutable, neighborName) =>
          FieldDef(
            mutable,
            Spanned(s"field$neighborName", Span.synth),
            TypeRef(neighborName, Span.synth),
            Span.synth
          )
        }
        TypeDef(
          spannedName,
          List(
            EnumCase(spannedName, fields, Span.synth)
          ),
          Span.synth
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
    assert(cycles.badSCCs === Set(0))
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

  test("Ensure strongly-connected components valid using Gen") {
    forAll(GenerateTypes.genTypesAux()) { typesAux =>
      val file = ParsedFile(GenerateTypes.toTypes(typesAux), Nil)
      val cycles = Cycles.fromFile(file)

      assert(cycles.sccs.size === typesAux.size)

      val bindings = Bindings.fromFile(file)
      for {
        typ <- file.typeDefs
        field <- typ.cases.head.fields
      } do {
        bindings.getType(field.typ) match {
          case td: TypeDef =>
            assert(
              cycles.sccMap(typ) <= cycles.sccMap(td),
              typesAux.mkString("\n") + "---\n" + cycles.sccs.map(_.map(_.name))
            )
          case _ => {}
        }
      }
    }
  }
}
