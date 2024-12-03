package fred

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import snapshot4s.scalatest.SnapshotAssertions
import org.scalacheck.Gen
import org.scalacheck.Shrink

class SCCTests
    extends AnyFunSuite, SnapshotAssertions, ScalaCheckPropertyChecks {

  /** This is just a helper to avoid typing out calls to the TypeDef ctor */
  def createFile(graph: Map[String, Set[(Boolean, String)]]): ParsedFile = {
    ParsedFile(
      GenUtil.toTypeDefs(
        graph.view
          .mapValues(_.map((mut, typeName) => (mut, s"f$typeName", typeName)))
          .toMap
      ),
      Nil
    )
  }

  def createFileImmutable(graph: Map[String, Set[String]]): ParsedFile = {
    createFile(graph.view.mapValues(_.map(false -> _)).toMap)
  }

  def simplifySCCs(sccs: List[Set[TypeDef]]): List[Set[String]] = {
    sccs.map(_.map(_.name))
  }

  test("Basic SCCs") {
    val file = createFileImmutable(Map(
      "A" -> Set("B", "C"),
      "B" -> Set("A"),
      "C" -> Set("D"),
      "D" -> Set("E"),
      "E" -> Set("F"),
      "F" -> Set("D")
    ))

    assert(
      List(Set("B", "A"), Set("C"), Set("D", "F", "E")) ===
        simplifySCCs(Cycles.fromFile(file).sccs)
    )
  }

  test("Multiple SCCs at same level") {
    val file = createFileImmutable(Map(
      "A" -> Set("B", "C", "D", "E"),
      "B" -> Set(),
      "C" -> Set("G"),
      "D" -> Set("G"),
      "E" -> Set("F"),
      "F" -> Set("E"),
      "G" -> Set()
    ))

    assert(
      List(Set("A"), Set("D"), Set("C"), Set("G"), Set("B"), Set("F", "E")) ===
        simplifySCCs(Cycles.fromFile(file).sccs)
    )
  }

  test("Is basic bad SCC detected") {
    val file = createFile(Map(
      "A" -> Set((true, "B")),
      "B" -> Set((false, "C")),
      "C" -> Set((false, "A"))
    ))

    val cycles = Cycles.fromFile(file)
    assert(cycles.sccs.size === 1)
    assert(cycles.badSCCs === Set(0))
  }

  test("Is basic good SCC not incorrectly marked bad") {
    val file = createFileImmutable(Map(
      "A" -> Set("B", "C", "D", "E"),
      "B" -> Set(),
      "C" -> Set("G"),
      "D" -> Set("G"),
      "E" -> Set("F"),
      "F" -> Set("E"),
      "G" -> Set("D")
    ))

    val cycles = Cycles.fromFile(file)
    assert(cycles.badSCCs.isEmpty)
  }

  def validateSccs(file: ParsedFile, cycles: Cycles) = {
    val allTypes = cycles.sccs.flatten
    assert(allTypes.distinct === allTypes)
    assert(allTypes.toSet == file.typeDefs.toSet)

    val bindings = Bindings.fromFile(file)

    // Ensure that SCCs are sorted
    for {
      typ <- file.typeDefs
      field <- typ.cases.head.fields
    } do {
      bindings.getType(field.typ) match {
        case td: TypeDef => assert(cycles.sccMap(typ) <= cycles.sccMap(td))
        case _           =>
      }
    }

    // All types in the same SCC should be able to reach other
    for {
      scc <- cycles.sccs
      from <- scc
      to <- scc
    } do { assert(reachable(from, to, bindings, Set.empty)) }
  }

  def reachable(
      from: TypeDef,
      to: TypeDef,
      bindings: Bindings,
      prev: Set[TypeDef]
  ): Boolean = {
    if (from == to) true
    else if (prev(from)) false
    else {
      val neighbors = from.cases.flatMap(_.fields).map(_.typ.name)
      if (neighbors.contains(to.name)) true
      else neighbors.exists { name =>
        bindings.types(name) match {
          case td: TypeDef => reachable(td, to, bindings, prev + from)
          case _           => false
        }
      }
    }
  }

  test("SCCs valid for totally random types") {
    // Generate types completely randomly and check if the SCCs make sense.
    // This doesn't check that the algorithm doesn't lump every single type
    // into the same SCC.
    forAll(
      Gen.sized { GenUtil.genTypesFullRandom(_) }.map(ParsedFile(_, Nil))
    ) { file =>
      val cycles = Cycles.fromFile(file)
      validateSccs(file, cycles)
    }
  }

  test("SCCs valid for not-so-arbitrary types") {
    // Ensure that the algorithm doesn't lump every type into the same SCC

    // Generate a bunch of types. Types that come later in the list can only have
    // references to types that come earlier in the list (or themselves).
    // Therefore, each type is its own strongly-connected component.
    val gen = Gen.sized { numSccs =>
      GenUtil.sequence(0.until(numSccs).map { i =>
        Gen.listOf(Gen.chooseNum(0, i)).map(GenUtil.GenTypeAux(_))
      })
    }

    forAll(gen) { typesAux =>
      val file = createFileImmutable(typesAux.zipWithIndex.map {
        (typesAux, i) => s"T$i" -> typesAux.refs.map(ref => s"T$ref").toSet
      }.toMap)
      val cycles = Cycles.fromFile(file)
      assert(cycles.sccs.size === typesAux.size)
      validateSccs(file, cycles)
    }
  }
}
