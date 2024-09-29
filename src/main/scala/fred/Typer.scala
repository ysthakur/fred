package fred

import scala.collection.mutable

case class Typer(types: Map[Expr, Type])

object Typer {

  /** Step through all expressions in the file and find their types
    */
  def resolveAllTypes(file: ParsedFile): Typer = {
    given ParsedFile = file
    val types = mutable.Map.empty[Expr, Type]
    for (fn <- file.fns) {
      resolveTypesInFn(fn, types)
    }
    Typer(types.toMap)
  }

  private def resolveTypesInFn(
      fn: FnDef,
      types: mutable.Map[Expr, Type]
  )(using ParsedFile) = {
    val paramBindings = fn.params.map { case Param(name, typ) =>
      name.value -> resolveType(typ.name, typ.span)
    }.toMap
    resolveExprType(fn.body, paramBindings, types)
  }

  /** Resolve the type for an expression, as well as all the expressions inside
    * it
    */
  private def resolveExprType(
      expr: Expr,
      bindings: Map[String, Type],
      types: mutable.Map[Expr, Type]
  )(using file: ParsedFile): Type = {
    expr match {
      case IntLiteral(_, _) =>
        types.put(expr, BuiltinType.Int)
        BuiltinType.Int
      case StringLiteral(_, _) =>
        types.put(expr, BuiltinType.Str)
        BuiltinType.Str
      case VarRef(name, _, span) =>
        bindings.get(name) match {
          case Some(typ) =>
            types.put(expr, typ)
            typ
          case None => throw new CompileError(s"No such variable: $name", span)
        }
      case FnCall(Spanned(name, nameSpan), args, _, _, span) =>
        file.fns.find(_.name.value == name) match {
          case Some(fn) =>
            for (arg <- args) {
              resolveExprType(arg, bindings, types)
            }
            val typ = resolveType(fn.returnType.name, fn.returnType.span)
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
          throw new CompileError(s"Condition type must be int, got ${condType.name}", cond.span)
        }
        val thenType = resolveExprType(thenBody, bindings, types)
        val elseType = resolveExprType(elseBody, bindings, types)
        if (thenType != elseType) {
          throw new CompileError(s"Branches have different types (${thenType.name} and ${elseType.name})", span)
        }
        types.put(expr, thenType)
        thenType
      case LetExpr(name, value, body, span) =>
        val varType = resolveExprType(value, bindings, types)
        types.put(expr, varType)
        val newBindings = bindings + (name.value -> varType)
        resolveExprType(body, newBindings, types)
      case MatchExpr(obj, arms, armsSpan) =>
        val obj
    }
  }

  private def resolveType(
      name: String,
      span: Span
  )(using file: ParsedFile): Type = {
    name match {
      case "int" => BuiltinType.Int
      case "str" => BuiltinType.Str
      case _ =>
        file.typeDefs.find(_.name.value == name) match {
          case Some(typ) => typ
          case None =>
            throw new CompileError(s"No such type: $name", span)
        }
    }
  }
}
