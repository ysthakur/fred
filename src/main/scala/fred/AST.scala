package fred

case class Span(start: Int, end: Int)

object Span {
  def synthetic = Span(-1, -1)
}

case class Spanned[T](value: T, span: Span)

case class ParsedFile(typeDefs: List[TypeDef], fns: List[FnDef])

case class Block(stmts: List[Stmt], span: Span)

enum Stmt {
  case ExprStmt(expr: Expr)
  case If(cond: Expr, thenBody: Block, elseBody: Block)
  case Print(expr: Expr)
}

case class FnDef(
    name: Spanned[String],
    params: List[Param],
    returnType: TypeRef,
    body: Block,
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

enum BinOp(val text: String) {
  case Plus extends BinOp("+")
  case Minus extends BinOp("-")
  case Mul extends BinOp("*")
  case Div extends BinOp("/")
  case Eq extends BinOp("=")
  case Lt extends BinOp("<")
  case Lteq extends BinOp("<=")
  case Gt extends BinOp(">")
  case Gteq extends BinOp(">=")
}

case class BinExpr(lhs: Expr, op: Spanned[BinOp], rhs: Expr, typ: Option[Type])
    extends Expr {
  override def span = Span(lhs.span.start, rhs.span.end)
}

case class MatchExpr(
    obj: Expr,
    cases: List[MatchCase],
    typ: Option[Type],
    span: Span
) extends Expr

case class MatchCase(pattern: MatchPattern, body: Block)

case class MatchPattern(
    ctorName: Spanned[String],
    bindings: List[Spanned[String]],
    span: Span
)
