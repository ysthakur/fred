package fred

import scala.collection.mutable

/** @param sccs
  *   A list of SCCs, sorted topologically
  * @param sccMap
  *   Map each type to the index of the SCC it occurs in
  * @param badSCCs
  *   SCCs where there is at least one mutable reference within that SCC to
  *   another type within the same SCC
  */
case class Cycles(
    sccs: List[Set[TypeDef]],
    sccMap: Map[TypeDef, Int],
    badSCCs: Set[Int]
)

object Cycles {
  def fromFile(file: ParsedFile): Cycles = {
    val sccs = findSCCs(file)
    val sccMap = sccs.zipWithIndex.flatMap { (types, i) =>
      types.map(_ -> i)
    }.toMap

    given bindings: Bindings = Bindings.fromFile(file)
    val badSCCs = sccs.zipWithIndex
      .filter { (scc, i) =>
        scc.exists { typ =>
          val fields = typ.cases.flatMap(_.fields)
          fields.exists { field =>
            bindings.getType(field.typ) match {
              case neighbor: TypeDef =>
                field.mutable && scc(neighbor)
              case _ => false
            }
          }
        }
      }
      .map(_._2)

    Cycles(sccs, sccMap, badSCCs.toSet)
  }

  /** Get a list of SCCs, sorted topologically. Uses Tarjan's strongly-connected
    * component algorithm
    */
  private def findSCCs(file: ParsedFile): List[Set[TypeDef]] = {
    given bindings: Bindings = Bindings.fromFile(file)

    var ind = 0
    val indexOf = mutable.Map.empty[TypeDef, Int]
    val lowlink = mutable.Map.empty[TypeDef, Int]
    val onStack = mutable.Set.empty[TypeDef]
    val S = mutable.ListBuffer.empty[TypeDef]

    val sccs = mutable.ListBuffer.empty[Set[TypeDef]]

    def strongConnect(v: TypeDef): Unit = {
      indexOf(v) = ind
      lowlink(v) = ind
      ind += 1
      S.prepend(v)
      onStack += v

      for (w <- outNeighbors(v)) {
        if (!indexOf.contains(w)) {
          // Successor w has not yet been visited; recurse on it
          strongConnect(w)
          lowlink(v) = lowlink(v).min(lowlink(w))
        } else if (onStack(w)) {
          // Successor w is in stack S and hence in the current SCC
          // If w is not on stack, then (v, w) is an edge pointing to an SCC already found and must be ignored
          // See below regarding the next line
          lowlink(v) = lowlink(v).min(indexOf(w))
        }
      }

      if (lowlink(v) == indexOf(v)) {
        val scc = mutable.ListBuffer.empty[TypeDef]

        while {
          val w = S.remove(0)
          onStack.remove(w)
          scc += w

          w != v
        } do {}

        sccs += scc.toSet
      }
    }

    for (typ <- file.typeDefs) {
      if (!indexOf.contains(typ)) {
        strongConnect(typ)
      }
    }

    sccs.toList.reverse
  }

  private def outNeighbors(
      typ: TypeDef
  )(using bindings: Bindings): List[TypeDef] = typ.cases
    .flatMap(_.fields.map(field => bindings.getType(field.typ)))
    .collect { case td: TypeDef => td }
    .distinct
}
