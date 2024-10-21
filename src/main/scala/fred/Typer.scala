package fred

import scala.collection.mutable

case class Typer(types: Map[Expr, Type])

case class Bindings(
    types: Map[String, Type],
    ctors: Map[String, (TypeDef, EnumCase)],
    fns: Map[String, FnDef],
    vars: Map[String, VarDef]
) {
  def enterFn(fn: FnDef): Bindings = {
    val paramBindings = fn.params.map { param =>
      param.name.value -> VarDef.Param(param, this.getType(param.typ))
    }.toMap
    this.copy(vars = this.vars ++ paramBindings)
  }

  def enterPattern(
      matchExpr: MatchExpr,
      pat: MatchPattern,
      typ: TypeDef
  ): Bindings = {
    val newVars = mutable.Map.from(this.vars)

    val ctor = typ.cases.find(_.name.value == pat.ctorName.value) match {
      case Some(variant) => variant
      case None =>
        throw new CompileError(
          s"No such constructor: ${pat.ctorName.value}",
          pat.ctorName.span
        )
    }

    for ((fieldName, varName) <- pat.bindings) {
      val typ =
        ctor.fields.find(_.name.value == fieldName.value) match {
          case Some(fieldDef) => this.getType(fieldDef.typ)
          case None =>
            throw new CompileError(
              s"No such field: ${fieldName.value}",
              fieldName.span
            )
        }
      newVars.put(
        varName.value,
        VarDef.Pat(matchExpr, pat, fieldName.value, varName.value, typ)
      )
    }

    this.copy(vars = newVars.toMap)
  }

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

object Bindings {
  def fromFile(file: ParsedFile): Bindings = {
    Bindings(
      types = file.typeDefs.map(typeDef => (typeDef.name, typeDef)).toMap
        ++ Map("int" -> BuiltinType.Int, "str" -> BuiltinType.Str),
      ctors = file.typeDefs.flatMap { typeDef =>
        typeDef.cases.map(variant => (variant.name.value, (typeDef, variant)))
      }.toMap,
      fns = file.fns.map(fn => (fn.name.value, fn)).toMap,
      vars = Map.empty
    )
  }
}

object Typer {

  /** Step through all expressions in the file and find their types
    */
  def resolveAllTypes(file: ParsedFile): Typer = {
    given ParsedFile = file
    val types = mutable.Map.empty[Expr, Type]
    val bindings = Bindings.fromFile(file)
    for (fn <- file.fns) {
      val got = resolveExprType(fn.body, bindings.enterFn(fn), types)
      if (got.name != fn.returnType.name) {
        throw new CompileError(
          s"Expected ${fn.returnType.name}, got ${got.name}",
          fn.returnType.span
        )
      }
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
      case varRef @ VarRef(_, _, _) =>
        val typ = bindings.getVar(varRef).typ
        types.put(expr, typ)
        typ
      case SetFieldExpr(obj, field, value, span) =>
        val objType = bindings.getVar(VarRef(obj.value, None, obj.span)).typ match {
          case td: TypeDef => td
          case builtinType => throw new CompileError(s"Can't set fields on $builtinType", obj.span)
        }
        if (!objType.hasCommonField(field.value)) {
          throw new CompileError(
            s"${field.value} isn't a common field in ${objType.name}",
            field.span
          )
        }
        val fieldTypeRef =
          objType.cases.head.fields.find(_.name.value == field.value).get.typ
        val typ = bindings.getType(fieldTypeRef)
        resolveExprType(value, bindings, types)
        types.put(expr, typ)
        typ

      case FieldAccess(obj, field, typ) =>
        val objType = resolveExprType(obj, bindings, types) match {
          case BuiltinType.Str =>
            throw new CompileError("Strings don't have fields", obj.span)
          case BuiltinType.Int =>
            throw new CompileError("Ints don't have fields", obj.span)
          case td: TypeDef => td
        }
        if (!objType.hasCommonField(field.value)) {
          throw new CompileError(
            s"${field.value} isn't a common field in ${objType.name}",
            field.span
          )
        }
        val fieldTypeRef =
          objType.cases.head.fields.find(_.name.value == field.value).get.typ
        val typ = bindings.getType(fieldTypeRef)
        types.put(expr, typ)
        typ
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
            if (name == "printf") {
              types.put(expr, BuiltinType.Int)
              BuiltinType.Int
            } else {
              throw new CompileError(
                s"No such function: $name",
                nameSpan
              )
            }
        }
      case CtorCall(ctorName, values, span) =>
        val (typ, variant) = bindings.ctors(ctorName.value)
        for ((fieldName, value) <- values) {
          val field = variant.fields.find(_.name.value == fieldName.value).get
          val gotType = resolveExprType(value, bindings, types)
          val expectedType = bindings.getType(field.typ)
          if (expectedType != gotType) {
            throw new CompileError(
              s"Expected ${expectedType.name}, got ${gotType.name}",
              span
            )
          }
        }
        types.put(expr, typ)
        typ
      case BinExpr(lhs, op, rhs, typ) =>
        op.value match {
          case BinOp.Plus | BinOp.Minus | BinOp.Mul | BinOp.Div | BinOp.Eq |
              BinOp.Lt | BinOp.Lteq | BinOp.Gt | BinOp.Gteq =>
            if (resolveExprType(lhs, bindings, types) != BuiltinType.Int) {
              throw new CompileError(
                s"Operator ${op.value.text} takes ints",
                lhs.span
              )
            }
            if (resolveExprType(rhs, bindings, types) != BuiltinType.Int) {
              throw new CompileError(
                s"Operator ${op.value.text} takes ints",
                rhs.span
              )
            }
            types.put(expr, BuiltinType.Int)
            BuiltinType.Int
          case BinOp.Seq =>
            resolveExprType(lhs, bindings, types)
            val typ = resolveExprType(rhs, bindings, types)
            types.put(expr, typ)
            typ
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
        val objType = resolveExprType(obj, bindings, types) match {
          case td: TypeDef => td
          case BuiltinType.Str =>
            throw new CompileError(
              "Can't match against strings",
              expr.span
            )
          case BuiltinType.Int =>
            throw new CompileError("Can't match against ints", expr.span)
        }
        // TODO this doesn't check that all variants are covered
        val armTypes = arms.map {
          case MatchArm(
                pat,
                body,
                span
              ) =>
            val armType = resolveExprType(
              body,
              bindings.enterPattern(matchExpr, pat, objType),
              types
            )
            (armType, span)
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
