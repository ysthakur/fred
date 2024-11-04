package fred

import scala.collection.mutable

/** Finds strongly-connected components in the graph of types using Kosaraju's
  * algorithm
  */
object SCC {
  def findSCCs(file: ParsedFile): List[Set[TypeDef]] = {
    given bindings: Bindings = Bindings.fromFile(file)
    val visited = mutable.Set.empty[TypeDef]
    def visit(types: List[TypeDef]): List[TypeDef] = {
      types.flatMap { typ =>
        if (visited(typ)) {
          Nil
        } else {
          visited += typ
          typ :: visit(outNeighbors(typ))
        }
      }
    }

    println(file.typeDefs.map(_.name))
    val L = visit(file.typeDefs)
    println(L.map(_.name))

    val inNeighbors = file.typeDefs.map(_ -> Set.empty[TypeDef]).to(mutable.Map)
    for (typ <- file.typeDefs) {
      for (neighbor <- outNeighbors(typ)) {
        inNeighbors(neighbor) += typ
      }
    }

    val componentFor = mutable.Map.empty[TypeDef, TypeDef]
    def assign(typ: TypeDef, root: TypeDef): Unit = {
      if (!componentFor.contains(typ)) {
        componentFor(typ) = root
        println(componentFor.map((td, root) => (td.name, root.name)))
        for (neighbor <- inNeighbors(typ)) {
          assign(neighbor, root)
        }
      }
    }

    for (typ <- L) {
      assign(typ, typ)
    }

    // These SCCs aren't sorted
    val sccs = file.typeDefs.groupBy(componentFor).values.toList.map(_.toSet)

    // todo sort these

    sccs
  }

  private def outNeighbors(
      typ: TypeDef
  )(using bindings: Bindings): List[TypeDef] = typ.cases
    .flatMap(_.fields.map(field => bindings.getType(field.typ)))
    .collect { case td: TypeDef => td }
    .distinct
}
