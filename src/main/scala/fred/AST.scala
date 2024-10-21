package fred

case class Span(start: Int, end: Int)

object Span {
  def synthetic = Span(-1, -1)
}

case class Spanned[T](value: T, span: Span)

case class ParsedFile(typeDefs: List[TypeDef], fns: List[FnDef])

case class MatchExpr(obj: Expr, arms: List[MatchArm], armsSpan: Span)
    extends Expr {
  override def span = Span(obj.span.start, armsSpan.end)

  def typ = None
}

case class MatchArm(pat: MatchPattern, body: Expr, span: Span)

/** For now, you can only have patterns that look like `Foo { a: a, b: b }`. No
  * nested constructors
  */
case class MatchPattern(
    ctorName: Spanned[String],
    bindings: List[(Spanned[String], Spanned[String])]
)

case class FnDef(
    name: Spanned[String],
    params: List[Param],
    returnType: TypeRef,
    body: Expr,
    span: Span
)

case class Param(name: Spanned[String], typ: TypeRef) {
  def span: Span = Span(name.span.start, typ.span.end)
}

sealed trait Expr {
  def typ: Option[Type]
  def span: Span
}

case class IntLiteral(value: Int, span: Span) extends Expr {
  override def typ = Some(BuiltinType.Int)
}

case class StringLiteral(value: String, span: Span) extends Expr {
  override def typ = Some(BuiltinType.Int)
}

sealed trait LHSExpr extends Expr

case class VarRef(name: String, typ: Option[Type], span: Span) extends LHSExpr

case class FieldAccess(obj: Expr, field: Spanned[String], typ: Option[Type])
    extends LHSExpr {
  override def span = Span(obj.span.start, field.span.end)
}

case class FnCall(
    fnName: Spanned[String],
    args: List[Expr],
    resolvedFn: Option[FnDef],
    typ: Option[Type],
    span: Span
) extends Expr

case class CtorCall(
    ctorName: Spanned[String],
    values: List[(Spanned[String], Expr)],
    span: Span
) extends Expr {
  def typ = None
}

enum BinOp(val text: String) {
  case Plus extends BinOp("+")
  case Minus extends BinOp("-")
  case Mul extends BinOp("*")
  case Div extends BinOp("/")
  case Eq extends BinOp("==")
  case Lt extends BinOp("<")
  case Lteq extends BinOp("<=")
  case Gt extends BinOp(">")
  case Gteq extends BinOp(">=")
  case Seq extends BinOp(";")
}

case class BinExpr(lhs: Expr, op: Spanned[BinOp], rhs: Expr, typ: Option[Type])
    extends Expr {
  override def span = Span(lhs.span.start, rhs.span.end)
}

case class LetExpr(name: Spanned[String], value: Expr, body: Expr, span: Span)
    extends Expr {
  override def typ = None
}

case class IfExpr(cond: Expr, thenBody: Expr, elseBody: Expr, span: Span)
    extends Expr {
  override def typ = None
}

/** An expression for setting a field on a variable
  */
case class SetFieldExpr(
    lhsObj: Spanned[String],
    lhsField: Spanned[String],
    value: Expr,
    span: Span
) extends Expr {
  override def typ = None
}

/** What a variable reference can resolve to
  */
enum VarDef {
  case Let(expr: LetExpr, typ: Type)
  case Param(param: fred.Param, typ: Type)

  /** @param matchExpr
    *   The match expression inside which this pattern is
    * @param pat
    *   The specific pattern inside which the binding is
    * @param field
    *   The original name of the field inside that constructor
    * @param varName
    *   The name of the variable that the field's value is being bound to
    */
  case Pat(
      matchExpr: MatchExpr,
      pat: MatchPattern,
      field: String,
      varName: String,
      typ: Type
  )

  def typ: Type

  def name: String =
    this match {
      case Let(expr, _)             => expr.name.value
      case VarDef.Param(param, _)   => param.name.value
      case Pat(_, _, _, varName, _) => varName
    }
}
