package fred

import cats.parse.{Parser as P, Parser0 as P0}
import cats.parse.Rfc5234.{alpha, crlf, digit, lf, sp}

object Parser {
  def parse(code: String): ParsedFile = {
    ParserHelpers.fileParser.parseAll(code) match {
      case Right(parsed) => parsed
      case Left(err) =>
        val badCode = code.substring(0, err.failedAtOffset)
        throw RuntimeException(s"""|Failed: $err
                                   |Code: $badCode""".stripMargin)
    }
  }

  private object ParserHelpers {
    val comment = (P.string("//") *> P.repUntil0(P.anyChar.void, crlf | lf)).void
    val ws: P0[Unit] = (sp | crlf | lf | comment).rep0.void

    extension [A](p1: P[A])
      /** Like `~`, but allows whitespace in between */
      def ~~[B](p2: P0[B]): P[(A, B)] = (p1 <* ws) ~ p2

    extension [A](p1: P0[A])
      /** Like `~`, but allows whitespace in between */
      def ~~[B](p2: P0[B]): P0[(A, B)] = (p1 <* ws) ~ p2

    val Keywords =
      Set("data", "fn", "match", "let", "in", "if", "then", "else", "set")

    // Not using Char.isUnicodeIentifierStart/Part for simplicity
    val idStart = alpha | P.char('_')
    val idPart = alpha | digit | P.char('_')
    val id: P[String] = (idStart ~ idPart.rep0).string
      .filter(!Keywords.contains(_)).backtrack.withContext("identifier")

    val spannedId: P[Spanned[String]] = spanned(id)

    def spanned[A](parser: P[A]): P[Spanned[A]] =
      (P.index.with1 ~ parser ~ P.index).map { case (start -> parsed -> end) =>
        Spanned(parsed, Span(start, end))
      }

    def keyword(kw: String): P[Unit] = P.string(kw).soft *> P.peek(!idPart)
    // id.backtrack.filter(kw == _) *> P.unit

    def inParens[A](parser: P0[A]): P[A] = P.char('(') *> ws *> parser <* ws <*
      P.char(')')
    def inBraces[A](parser: P0[A]): P[A] = P.char('{') *> ws *> parser <* ws <*
      P.char('}')

    val expr: P[Expr] = P.defer(exprLazy <* ws)

    val letExpr: P[Expr] =
      (spanned(
        (keyword("let") *> ws *> spannedId <* ws <* P.char('=') <* ws) ~
          (expr <* ws <* keyword("in") <* ws)
      ) ~ expr).map { case (Spanned((name, value), span), body) =>
        LetExpr(name, value, body, span)
      }.withContext("let expr")

    val ifExpr: P[Expr] = spanned(
      keyword("if") *> ws *> (expr <* ws <* keyword("then") <* ws) ~
        (expr <* ws <* keyword("else") <* ws) ~ expr
    ).map { case Spanned(cond -> thenBody -> elseBody, span) =>
      IfExpr(cond, thenBody, elseBody, span)
    }.withContext("if expr")

    val intLiteral: P[Expr] = (digit.rep.string ~ P.index).map {
      case (num, end) => IntLiteral(num.toInt, Span(end - num.length, end))
    }.withContext("int literal")
    val stringLiteral: P[StringLiteral] = spanned(
      P.char('"') *> ((P.char('\\') ~ P.anyChar) | (P.charWhere(_ != '"'))).rep
        .string <* P.char('"')
    ).map { case Spanned(text, span) => StringLiteral(text, span) }
      .withContext("str literal")
    val literal = intLiteral | stringLiteral
    val parenExpr = inParens(expr).withContext("paren expr")
    val varRefOrFnCallOrCtorCall =
      (P.index.with1 ~ (id <* ws) ~ inParens(expr.repSep0(P.char(',') *> ws))
        .eitherOr(inBraces(
          ((spannedId <* ws <* P.char(':') <* ws) ~ expr)
            .repSep0(P.char(',') *> ws)
        )).? ~ P.index).map {
        case (start -> name -> None -> end) =>
          VarRef(name, Span(start, start + name.length))
        case (start -> name -> Some(Right(args)) -> end) => FnCall(
            Spanned(name, Span(start, start + name.length)),
            args,
            None,
            None,
            Span(start, end)
          )
        case (start -> name -> Some(Left(values)) -> end) => CtorCall(
            Spanned(name, Span(start, start + name.length)),
            values,
            Span(start, end)
          )
      }
    val selectable: P[Expr] = varRefOrFnCallOrCtorCall | parenExpr | literal
    val fieldAccess: P[Expr] =
      ((selectable <* ws) ~ (P.char('.') *> ws *> spannedId <* ws).rep0).map {
        case (obj, fields) => fields.foldLeft(obj) { (obj, field) =>
            FieldAccess(obj, field, None)
          }
      }
    val binOp1 = binOp(fieldAccess, P.stringIn(List("*", "/")))
    val binOp2 = binOp(binOp1, P.stringIn(List("+", "-")))
    val binOp3 = binOp(binOp2, P.stringIn(List(">=", "<=", ">", "<", "==")))

    val matchPattern: P[MatchPattern] =
      ((spannedId <* ws) ~ inBraces(
        ((spannedId <* ws <* P.char(':') <* ws) ~ spannedId <* ws)
          .repSep0(P.char(',') <* ws)
      )).map { case (ctorName, bindings) => MatchPattern(ctorName, bindings) }
    val matchArm: P[MatchArm] =
      spanned((matchPattern <* ws <* P.string("=>") <* ws) ~ expr).map {
        case Spanned((pat, body), span) => MatchArm(pat, body, span)
      }.withContext("match arm")
    val matchExpr =
      ((binOp3 <* ws) ~ spanned(
        keyword("match") *>
          ws *> inBraces((matchArm <* ws).repSep0(P.char(',') *> ws)) <* ws
      ).rep0).map { case (obj, matches) =>
        matches.foldLeft(obj) { case (obj, Spanned(matchArms, armsSpan)) =>
          MatchExpr(obj, matchArms, armsSpan)
        }
      }

    val setFieldExpr: P[Expr] = P.defer(
      ((P.index.soft <* keyword("set") <* ws).with1 ~
        (spannedId <* ws <* P.char('.') <* ws) ~ (spannedId <* ws) ~
        seqAbleExpr).map { case (start -> lhsVar -> lhsField -> value) =>
        SetFieldExpr(lhsVar, lhsField, value, Span(start, value.span.end))
      }
    )

    val seqAbleExpr: P[Expr] = ifExpr | matchExpr | setFieldExpr
    val seqExpr = binOp(seqAbleExpr, P.stringIn(List(";")))
    def binOp(prev: P[Expr], op: P[String]): P[Expr] =
      ((prev <* ws) ~ (op ~ (P.index <* ws) ~ prev <* ws).rep0).map {
        case (left, reps) => reps
            .foldLeft(left) { case (lhs, op -> opEnd -> rhs) =>
              val binop = BinOp.values.find(_.text == op).get
              BinExpr(
                lhs,
                Spanned(binop, Span(opEnd - op.length, opEnd)),
                rhs,
                None
              )
            }
      }

    lazy val exprLazy: P[Expr] = letExpr | seqExpr

    val typeRef: P[TypeRef] = (P.index.with1 ~ id).map { case (start, name) =>
      TypeRef(name, Span(start, start + name.length))
    }

    val param: P[Param] = ((spannedId <* ws <* P.char(':') <* ws) ~ typeRef)
      .map(Param(_, _))

    val fnDef: P[FnDef] =
      ((P.index.with1 <* keyword("fn") <* ws) ~
        (spannedId <* ws <* P.char('(') <* ws) ~
        ((param <* ws).repSep0(P.char(',') *> ws) <* ws <* P.char(')') <* ws) ~
        (P.char(':') *> ws *> typeRef <* ws) ~
        (P.char('=') *> ws *> expr)).map {
        case (start -> name -> params -> returnType -> body) =>
          FnDef(name, params, returnType, body, Span(start, body.span.end))
      }

    val fieldDef: P[FieldDef] =
      ((P.index.soft ~ (keyword("mut") *> ws).backtrack.?).with1 ~
        (spannedId <* ws <* P.char(':') <* ws) ~ typeRef).map {
        case (start -> mutable -> name -> typ) =>
          FieldDef(mutable.isDefined, name, typ, Span(start, typ.span.end))
      }

    val enumCase: P[EnumCase] =
      ((spannedId <* ws) ~ inBraces(
        (fieldDef <* ws).repSep0(P.char(',') *> ws)
      ) ~ P.index <* ws).map { case (name -> fields -> end) =>
        EnumCase(name, fields, Span(name.span.start, end))
      }

    val enumDef: P[TypeDef] =
      (P.index.with1.soft ~
        (keyword("data") *> ws *> spannedId <* ws <* P.char('=') <* ws) ~
        enumCase.repSep0(P.char('|') *> ws) ~ P.index <* ws).map {
        case (start -> name -> cases -> end) =>
          TypeDef(name, cases, Span(start, end))
      }

    val fileParser = (ws *> (enumDef <* ws).rep0 ~ (fnDef <* ws).rep0)
      .map(ParsedFile(_, _))
    // (ws *> (fnDef <* ws).rep0).map(ParsedFile(Nil, _))
  }
}
