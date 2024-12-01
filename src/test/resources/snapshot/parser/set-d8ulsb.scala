ParsedFile(
  typeDefs = List(),
  fns = List(
    FnDef(
      name = Spanned(value = "foo", span = Span(start = 3, end = 6)),
      params = List(),
      returnType = TypeRef(name = "int", span = Span(start = 10, end = 13)),
      body = BinExpr(
        lhs = SetFieldExpr(
          lhsObj = Spanned(value = "foo", span = Span(start = 20, end = 23)),
          lhsField = Spanned(value = "bar", span = Span(start = 24, end = 27)),
          value = IntLiteral(value = 4, span = Span(start = 28, end = 29)),
          span = Span(start = 16, end = 29)
        ),
        op = Spanned(value = Seq, span = Span(start = 29, end = 30)),
        rhs = VarRef(name = "foo", span = Span(start = 31, end = 34)),
        typ = None
      ),
      span = Span(start = 0, end = 34)
    )
  )
)