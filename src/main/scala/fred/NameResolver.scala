package fred

object NameResolver {

  /** Find [[TypeRef]]s that didn't resolve to anything */
  def checkTypes(file: ParsedFile): List[TypeRef] = {
    val fieldTypes = file.typeDefs.flatMap(_.cases.flatMap(_.fields.map(_.typ)))
    val fnTypes = file.fns.flatMap { fn =>
      fn.returnType :: fn.params.map(_.typ)
    }
    val allTypes = fieldTypes ::: fnTypes
    allTypes.filter(typ => file.typeDefs.forall(_.name != typ.name))
  }
}
