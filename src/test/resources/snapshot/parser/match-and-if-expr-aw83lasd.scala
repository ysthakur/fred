ParsedFile(
  typeDefs = List(),
  fns = List(
    FnDef(
      name = Spanned(value = "foo", span = Span(start = 10, end = 13)),
      params = List(),
      returnType = TypeRef(name = "int", span = Span(start = 17, end = 20)),
      body = IfExpr(
        cond = IntLiteral(value = 1, span = Span(start = 34, end = 35)),
        thenBody = IntLiteral(value = 2, span = Span(start = 41, end = 42)),
        elseBody = MatchExpr(
          obj = IntLiteral(value = 3, span = Span(start = 66, end = 67)),
          arms = List(),
          armsSpan = Span(start = 68, end = 94)
        ),
        span = Span(start = 31, end = 94)
      ),
      span = Span(start = 7, end = 94)
    )
  )
)