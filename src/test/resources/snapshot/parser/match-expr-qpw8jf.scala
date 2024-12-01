ParsedFile(
  typeDefs = List(),
  fns = List(
    FnDef(
      name = Spanned(value = "foo", span = Span(start = 10, end = 13)),
      params = List(),
      returnType = TypeRef(name = "int", span = Span(start = 17, end = 20)),
      body = MatchExpr(
        obj = MatchExpr(
          obj = BinExpr(
            lhs = VarRef(name = "foo", span = Span(start = 31, end = 34)),
            op = Spanned(value = Plus, span = Span(start = 35, end = 36)),
            rhs = IntLiteral(value = 2, span = Span(start = 37, end = 38)),
            typ = None
          ),
          arms = List(
            MatchArm(
              pat = MatchPattern(
                ctorName = Spanned(value = "Foo", span = Span(start = 57, end = 60)),
                bindings = List(
                  (
                    Spanned(value = "a", span = Span(start = 63, end = 64)),
                    Spanned(value = "b", span = Span(start = 66, end = 67))
                  ),
                  (
                    Spanned(value = "c", span = Span(start = 69, end = 70)),
                    Spanned(value = "d", span = Span(start = 72, end = 73))
                  )
                )
              ),
              body = BinExpr(
                lhs = IntLiteral(value = 7, span = Span(start = 79, end = 80)),
                op = Spanned(value = Plus, span = Span(start = 81, end = 82)),
                rhs = IntLiteral(value = 2, span = Span(start = 83, end = 84)),
                typ = None
              ),
              span = Span(start = 57, end = 84)
            ),
            MatchArm(
              pat = MatchPattern(
                ctorName = Spanned(value = "Bar", span = Span(start = 96, end = 99)),
                bindings = List()
              ),
              body = VarRef(name = "empty", span = Span(start = 107, end = 112)),
              span = Span(start = 96, end = 112)
            ),
            MatchArm(
              pat = MatchPattern(
                ctorName = Spanned(value = "Blech", span = Span(start = 124, end = 129)),
                bindings = List(
                  (
                    Spanned(value = "a", span = Span(start = 132, end = 133)),
                    Spanned(value = "b", span = Span(start = 135, end = 136))
                  )
                )
              ),
              body = FnCall(
                fnName = Spanned(value = "single", span = Span(start = 142, end = 148)),
                args = List(VarRef(name = "thing", span = Span(start = 149, end = 154))),
                resolvedFn = None,
                typ = None,
                span = Span(start = 142, end = 155)
              ),
              span = Span(start = 124, end = 164)
            )
          ),
          armsSpan = Span(start = 39, end = 166)
        ),
        arms = List(
          MatchArm(
            pat = MatchPattern(
              ctorName = Spanned(value = "ChainedMatch", span = Span(start = 184, end = 196)),
              bindings = List()
            ),
            body = FnCall(
              fnName = Spanned(value = "just", span = Span(start = 204, end = 208)),
              args = List(VarRef(name = "because", span = Span(start = 209, end = 216))),
              resolvedFn = None,
              typ = None,
              span = Span(start = 204, end = 217)
            ),
            span = Span(start = 184, end = 226)
          )
        ),
        armsSpan = Span(start = 166, end = 234)
      ),
      span = Span(start = 7, end = 234)
    )
  )
)