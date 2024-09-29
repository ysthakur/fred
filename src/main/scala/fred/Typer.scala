package fred

import scala.collection.mutable

case class Typer(types: Map[Expr, Type])

case class Bindings(
    types: Map[String, Type],
    ctors: Map[String, (TypeDef, EnumCase)],
    fns: Map[String, FnDef],
    vars: Map[String, VarDef]
) {
  def withVar(varName: String, varDef: VarDef): Bindings =
    this.copy(vars = this.vars + (varName -> varDef))

  def getVar(varRef: VarRef): VarDef =
    vars.get(varRef.name) match {
      case Some(varDef) => varDef
      case None =>
        throw new CompileError(s"Unknown variable: ${varRef.name}", varRef.span)
    }

  def getType(typeRef: TypeRef): Type =
    types.get(typeRef.name) match {
      case Some(typ) => typ
      case None =>
        throw new CompileError(s"No such type: ${typeRef.name}", typeRef.span)
    }
}

object Typer {

  /** Step through all expressions in the file and find their types
    */
  def resolveAllTypes(file: ParsedFile): Typer = {
    given ParsedFile = file
    val types = mutable.Map.empty[Expr, Type]
    val bindings = Bindings(
      types = file.typeDefs.map(typeDef => (typeDef.name, typeDef)).toMap
        ++ Map("int" -> BuiltinType.Int, "str" -> BuiltinType.Str),
      ctors = file.typeDefs.flatMap { typeDef =>
        typeDef.cases.map(variant => (variant.name.value, (typeDef, variant)))
      }.toMap,
      fns = file.fns.map(fn => (fn.name.value, fn)).toMap,
      vars = Map.empty
    )
    for (fn <- file.fns) {
      val paramBindings = fn.params.map { param =>
        param.name.value -> VarDef.Param(param, bindings.getType(param.typ))
      }.toMap
      val newBindings = bindings.copy(vars = bindings.vars ++ paramBindings)
      resolveExprType(fn.body, newBindings, types)
    }
    Typer(types.toMap)
  }

  /** Resolve the type for an expression, as well as all the expressions inside
    * it
    */
  private def resolveExprType(
      expr: Expr,
      bindings: Bindings,
      types: mutable.Map[Expr, Type]
  )(using file: ParsedFile): Type = {
    expr match {
      case IntLiteral(_, _) =>
        types.put(expr, BuiltinType.Int)
        BuiltinType.Int
      case StringLiteral(_, _) =>
        types.put(expr, BuiltinType.Str)
        BuiltinType.Str
      case varRef @ VarRef(_, _, _) => bindings.getVar(varRef).typ
      case FnCall(Spanned(name, nameSpan), args, _, _, span) =>
        file.fns.find(_.name.value == name) match {
          case Some(fn) =>
            for (arg <- args) {
              resolveExprType(arg, bindings, types)
            }
            val typ = bindings.getType(fn.returnType)
            types.put(expr, typ)
            typ
          case None =>
            throw new CompileError(
              s"No such function: $name",
              nameSpan
            )
        }
      case IfExpr(cond, thenBody, elseBody, span) =>
        val condType = resolveExprType(cond, bindings, types)
        if (condType != BuiltinType.Int) {
          throw new CompileError(
            s"Condition type must be int, got ${condType.name}",
            cond.span
          )
        }
        val thenType = resolveExprType(thenBody, bindings, types)
        val elseType = resolveExprType(elseBody, bindings, types)
        if (thenType != elseType) {
          throw new CompileError(
            s"Branches have different types (${thenType.name} and ${elseType.name})",
            span
          )
        }
        types.put(expr, thenType)
        thenType
      case expr @ LetExpr(name, value, body, span) =>
        val varType = resolveExprType(value, bindings, types)
        val newBindings =
          bindings.withVar(name.value, VarDef.Let(expr, varType))
        val bodyType = resolveExprType(body, newBindings, types)
        types.put(expr, bodyType)
        bodyType
      case matchExpr @ MatchExpr(obj, arms, armsSpan) =>
        val objType = resolveExprType(obj, bindings, types)
        val variants = objType match {
          case TypeDef(typeName, variants, _) => variants
          case BuiltinType.Str =>
            throw new CompileError("Can't match against strings", expr.span)
          case BuiltinType.Int =>
            throw new CompileError("Can't match against ints", expr.span)
        }
        val armTypes = arms.map {
          case MatchArm(
                pat @ MatchPattern(ctorName, patBindings),
                body,
                span
              ) =>
            val ctor = variants.find(_.name.value == ctorName.value) match {
              case Some(variant) => variant
              case None =>
                throw new CompileError(
                  s"No such constructor: ${ctorName.value}",
                  ctorName.span
                )
            }
            val newBindings = patBindings.foldLeft(bindings) {
              case (oldBindings, (fieldName, varName)) =>
                val typ =
                  ctor.fields.find(_.name.value == fieldName.value) match {
                    case Some(fieldDef) => bindings.getType(fieldDef.typ)
                    case None =>
                      throw new CompileError(
                        s"No such field: ${fieldName.value}",
                        fieldName.span
                      )
                  }
                oldBindings.withVar(
                  varName.value,
                  VarDef.Pat(matchExpr, pat, fieldName.value, typ)
                )
            }
            (resolveExprType(body, newBindings, types), span)
        }

        val (firstArmType, _) = armTypes.head

        for ((typ, span) <- armTypes.tail) {
          if (typ != firstArmType) {
            throw new CompileError(
              s"Expected arm type to be ${firstArmType.name}, got ${typ.name}",
              span
            )
          }
        }

        types.put(matchExpr, firstArmType)
        firstArmType
    }
  }
}
