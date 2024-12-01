List(
  (
    LetExpr(
      name = Spanned(value = "x", span = Span(start = 221, end = 222)),
      value = IntLiteral(value = 2, span = Span(start = 225, end = 226)),
      body = BinExpr(
        lhs = MatchExpr(
          obj = VarRef(name = "param", span = Span(start = 238, end = 243)),
          arms = List(
            MatchArm(
              pat = MatchPattern(
                ctorName = Spanned(value = "Bar", span = Span(start = 262, end = 265)),
                bindings = List(
                  (
                    Spanned(value = "foo", span = Span(start = 268, end = 271)),
                    Spanned(value = "blech", span = Span(start = 273, end = 278))
                  )
                )
              ),
              body = VarRef(name = "blech", span = Span(start = 284, end = 289)),
              span = Span(start = 262, end = 289)
            ),
            MatchArm(
              pat = MatchPattern(
                ctorName = Spanned(value = "Baz", span = Span(start = 301, end = 304)),
                bindings = List(
                  (
                    Spanned(value = "blech", span = Span(start = 307, end = 312)),
                    Spanned(value = "blech", span = Span(start = 314, end = 319))
                  )
                )
              ),
              body = VarRef(name = "param", span = Span(start = 325, end = 330)),
              span = Span(start = 301, end = 339)
            )
          ),
          armsSpan = Span(start = 244, end = 340)
        ),
        op = Spanned(value = Seq, span = Span(start = 340, end = 341)),
        rhs = VarRef(name = "x", span = Span(start = 350, end = 351)),
        typ = None
      ),
      span = Span(start = 217, end = 238)
    ),
    Int
  ),
  (IntLiteral(value = 2, span = Span(start = 225, end = 226)), Int),
  (
    VarRef(name = "param", span = Span(start = 238, end = 243)),
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
              typ = TypeRef(name = "str", span = Span(start = 128, end = 131)),
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
      span = Span(start = 7, end = 183)
    )
  ),
  (
    MatchExpr(
      obj = VarRef(name = "param", span = Span(start = 238, end = 243)),
      arms = List(
        MatchArm(
          pat = MatchPattern(
            ctorName = Spanned(value = "Bar", span = Span(start = 262, end = 265)),
            bindings = List(
              (
                Spanned(value = "foo", span = Span(start = 268, end = 271)),
                Spanned(value = "blech", span = Span(start = 273, end = 278))
              )
            )
          ),
          body = VarRef(name = "blech", span = Span(start = 284, end = 289)),
          span = Span(start = 262, end = 289)
        ),
        MatchArm(
          pat = MatchPattern(
            ctorName = Spanned(value = "Baz", span = Span(start = 301, end = 304)),
            bindings = List(
              (
                Spanned(value = "blech", span = Span(start = 307, end = 312)),
                Spanned(value = "blech", span = Span(start = 314, end = 319))
              )
            )
          ),
          body = VarRef(name = "param", span = Span(start = 325, end = 330)),
          span = Span(start = 301, end = 339)
        )
      ),
      armsSpan = Span(start = 244, end = 340)
    ),
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
              typ = TypeRef(name = "str", span = Span(start = 128, end = 131)),
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
      span = Span(start = 7, end = 183)
    )
  ),
  (
    BinExpr(
      lhs = MatchExpr(
        obj = VarRef(name = "param", span = Span(start = 238, end = 243)),
        arms = List(
          MatchArm(
            pat = MatchPattern(
              ctorName = Spanned(value = "Bar", span = Span(start = 262, end = 265)),
              bindings = List(
                (
                  Spanned(value = "foo", span = Span(start = 268, end = 271)),
                  Spanned(value = "blech", span = Span(start = 273, end = 278))
                )
              )
            ),
            body = VarRef(name = "blech", span = Span(start = 284, end = 289)),
            span = Span(start = 262, end = 289)
          ),
          MatchArm(
            pat = MatchPattern(
              ctorName = Spanned(value = "Baz", span = Span(start = 301, end = 304)),
              bindings = List(
                (
                  Spanned(value = "blech", span = Span(start = 307, end = 312)),
                  Spanned(value = "blech", span = Span(start = 314, end = 319))
                )
              )
            ),
            body = VarRef(name = "param", span = Span(start = 325, end = 330)),
            span = Span(start = 301, end = 339)
          )
        ),
        armsSpan = Span(start = 244, end = 340)
      ),
      op = Spanned(value = Seq, span = Span(start = 340, end = 341)),
      rhs = VarRef(name = "x", span = Span(start = 350, end = 351)),
      typ = None
    ),
    Int
  ),
  (
    VarRef(name = "blech", span = Span(start = 284, end = 289)),
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
              typ = TypeRef(name = "str", span = Span(start = 128, end = 131)),
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
      span = Span(start = 7, end = 183)
    )
  ),
  (
    VarRef(name = "param", span = Span(start = 325, end = 330)),
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
              typ = TypeRef(name = "str", span = Span(start = 128, end = 131)),
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
      span = Span(start = 7, end = 183)
    )
  ),
  (VarRef(name = "x", span = Span(start = 350, end = 351)), Int)
)