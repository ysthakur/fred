ParsedFile(
  typeDefs = List(),
  fns = List(
    FnDef(
      name = Spanned(value = "foo", span = Span(start = 10, end = 13)),
      params = List(
        Param(
          name = Spanned(value = "param", span = Span(start = 14, end = 19)),
          typ = TypeRef(name = "ParamType", span = Span(start = 21, end = 30))
        ),
        Param(
          name = Spanned(value = "bleh", span = Span(start = 32, end = 36)),
          typ = TypeRef(name = "int", span = Span(start = 38, end = 41))
        )
      ),
      returnType = TypeRef(name = "int", span = Span(start = 44, end = 47)),
      body = LetExpr(
        name = Spanned(value = "x", span = Span(start = 62, end = 63)),
        value = IntLiteral(value = 3, span = Span(start = 66, end = 67)),
        body = BinExpr(
          lhs = FnCall(
            fnName = Spanned(value = "foo", span = Span(start = 79, end = 82)),
            args = List(
              VarRef(name = "bleh", span = Span(start = 83, end = 87)),
              BinExpr(
                lhs = IntLiteral(value = 4, span = Span(start = 89, end = 90)),
                op = Spanned(value = Plus, span = Span(start = 91, end = 92)),
                rhs = VarRef(name = "x", span = Span(start = 93, end = 94)),
                typ = None
              )
            ),
            resolvedFn = None,
            typ = None,
            span = Span(start = 79, end = 95)
          ),
          op = Spanned(value = Seq, span = Span(start = 95, end = 96)),
          rhs = VarRef(name = "x", span = Span(start = 105, end = 106)),
          typ = None
        ),
        span = Span(start = 58, end = 79)
      ),
      span = Span(start = 7, end = 79)
    )
  )
)