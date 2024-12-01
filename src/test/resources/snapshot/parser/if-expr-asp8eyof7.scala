ParsedFile(
  typeDefs = List(),
  fns = List(
    FnDef(
      name = Spanned(value = "foo", span = Span(start = 10, end = 13)),
      params = List(),
      returnType = TypeRef(name = "int", span = Span(start = 17, end = 20)),
      body = IfExpr(
        cond = VarRef(name = "foo", span = Span(start = 34, end = 37)),
        thenBody = IfExpr(
          cond = VarRef(name = "bar", span = Span(start = 56, end = 59)),
          thenBody = VarRef(name = "x", span = Span(start = 65, end = 66)),
          elseBody = VarRef(name = "y", span = Span(start = 72, end = 73)),
          span = Span(start = 53, end = 82)
        ),
        elseBody = IfExpr(
          cond = BinExpr(
            lhs = IntLiteral(value = 3, span = Span(start = 100, end = 101)),
            op = Spanned(value = Plus, span = Span(start = 102, end = 103)),
            rhs = IntLiteral(value = 2, span = Span(start = 104, end = 105)),
            typ = None
          ),
          thenBody = BinExpr(
            lhs = FnCall(
              fnName = Spanned(value = "bleh", span = Span(start = 123, end = 127)),
              args = List(),
              resolvedFn = None,
              typ = None,
              span = Span(start = 123, end = 129)
            ),
            op = Spanned(value = Seq, span = Span(start = 129, end = 130)),
            rhs = IntLiteral(value = 5, span = Span(start = 143, end = 144)),
            typ = None
          ),
          elseBody = BinExpr(
            lhs = FnCall(
              fnName = Spanned(value = "foo", span = Span(start = 172, end = 175)),
              args = List(),
              resolvedFn = None,
              typ = None,
              span = Span(start = 172, end = 177)
            ),
            op = Spanned(value = Seq, span = Span(start = 177, end = 178)),
            rhs = VarRef(name = "bar", span = Span(start = 191, end = 194)),
            typ = None
          ),
          span = Span(start = 97, end = 201)
        ),
        span = Span(start = 31, end = 201)
      ),
      span = Span(start = 7, end = 201)
    )
  )
)