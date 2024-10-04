ParsedFile(
  typeDefs = List(
    TypeDef(
      nameSpanned = Spanned(value = "Foo", span = Span(start = 12, end = 15)),
      cases = List(
        EnumCase(
          name = Spanned(value = "Bar", span = Span(start = 26, end = 29)),
          fields = List(
            FieldDef(
              mutable = true,
              name = Spanned(value = "foo", span = Span(start = 48, end = 51)),
              typ = TypeRef(name = "Foo", span = Span(start = 53, end = 56)),
              span = Span(start = 44, end = 56)
            ),
            FieldDef(
              mutable = false,
              name = Spanned(value = "fred", span = Span(start = 70, end = 74)),
              typ = TypeRef(name = "Fred", span = Span(start = 76, end = 80)),
              span = Span(start = 70, end = 80)
            )
          ),
          span = Span(start = 26, end = 92)
        ),
        EnumCase(
          name = Spanned(value = "Baz", span = Span(start = 103, end = 106)),
          fields = List(
            FieldDef(
              mutable = false,
              name = Spanned(value = "blech", span = Span(start = 121, end = 126)),
              typ = TypeRef(name = "Bar", span = Span(start = 128, end = 131)),
              span = Span(start = 121, end = 131)
            ),
            FieldDef(
              mutable = true,
              name = Spanned(value = "gah", span = Span(start = 149, end = 152)),
              typ = TypeRef(name = "int", span = Span(start = 154, end = 157)),
              span = Span(start = 145, end = 157)
            )
          ),
          span = Span(start = 103, end = 169)
        )
      ),
      span = Span(start = 7, end = 176)
    )
  ),
  fns = List()
)