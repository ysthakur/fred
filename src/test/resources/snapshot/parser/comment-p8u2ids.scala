ParsedFile(
  typeDefs = List(),
  fns = List(
    FnDef(
      name = Spanned(value = "foo", span = Span(start = 37, end = 40)),
      params = List(),
      returnType = TypeRef(name = "str", span = Span(start = 44, end = 47)),
      body = StringLiteral(value = "this is // not a comment", span = Span(start = 106, end = 132)),
      span = Span(start = 34, end = 132)
    )
  )
)