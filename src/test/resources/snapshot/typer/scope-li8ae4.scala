List(
  (
    IfExpr(
      cond = IntLiteral(value = 0, span = Span(start = 34, end = 35)),
      thenBody = LetExpr(
        name = Spanned(value = "x", span = Span(start = 55, end = 56)),
        value = IntLiteral(value = 2, span = Span(start = 59, end = 60)),
        body = VarRef(name = "x", typ = None, span = Span(start = 64, end = 65)),
        span = Span(start = 51, end = 64)
      ),
      elseBody = LetExpr(
        name = Spanned(value = "x", span = Span(start = 93, end = 94)),
        value = StringLiteral(value = "foo", span = Span(start = 97, end = 102)),
        body = LetExpr(
          name = Spanned(value = "y", span = Span(start = 120, end = 121)),
          value = VarRef(name = "x", typ = None, span = Span(start = 124, end = 125)),
          body = IntLiteral(value = 4, span = Span(start = 139, end = 140)),
          span = Span(start = 116, end = 139)
        ),
        span = Span(start = 89, end = 116)
      ),
      span = Span(start = 31, end = 147)
    ),
    Int
  ),
  (IntLiteral(value = 0, span = Span(start = 34, end = 35)), Int),
  (
    LetExpr(
      name = Spanned(value = "x", span = Span(start = 55, end = 56)),
      value = IntLiteral(value = 2, span = Span(start = 59, end = 60)),
      body = VarRef(name = "x", typ = None, span = Span(start = 64, end = 65)),
      span = Span(start = 51, end = 64)
    ),
    Int
  ),
  (IntLiteral(value = 2, span = Span(start = 59, end = 60)), Int),
  (VarRef(name = "x", typ = None, span = Span(start = 64, end = 65)), Int),
  (
    LetExpr(
      name = Spanned(value = "x", span = Span(start = 93, end = 94)),
      value = StringLiteral(value = "foo", span = Span(start = 97, end = 102)),
      body = LetExpr(
        name = Spanned(value = "y", span = Span(start = 120, end = 121)),
        value = VarRef(name = "x", typ = None, span = Span(start = 124, end = 125)),
        body = IntLiteral(value = 4, span = Span(start = 139, end = 140)),
        span = Span(start = 116, end = 139)
      ),
      span = Span(start = 89, end = 116)
    ),
    Int
  ),
  (StringLiteral(value = "foo", span = Span(start = 97, end = 102)), Str),
  (
    LetExpr(
      name = Spanned(value = "y", span = Span(start = 120, end = 121)),
      value = VarRef(name = "x", typ = None, span = Span(start = 124, end = 125)),
      body = IntLiteral(value = 4, span = Span(start = 139, end = 140)),
      span = Span(start = 116, end = 139)
    ),
    Int
  ),
  (VarRef(name = "x", typ = None, span = Span(start = 124, end = 125)), Str),
  (IntLiteral(value = 4, span = Span(start = 139, end = 140)), Int)
)