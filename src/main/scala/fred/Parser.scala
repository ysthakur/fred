package fred

import cats.parse.{Parser as P, Parser0 as P0}
import cats.parse.Rfc5234.{crlf, digit, lf, sp}

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
    val ws: P0[Unit] = (sp | crlf | lf).rep0.void

    extension [A](p1: P[A])
      /** Like `~`, but allows whitespace in between */
      def ~~[B](p2: P0[B]): P[(A, B)] = (p1 <* ws) ~ p2

    extension [A](p1: P0[A])
      /** Like `~`, but allows whitespace in between */
      def ~~[B](p2: P0[B]): P0[(A, B)] = (p1 <* ws) ~ p2

    val id: P[String] =
      (P.charWhere(_.isUnicodeIdentifierStart) ~ P.charsWhile0(
        _.isUnicodeIdentifierPart
      )).map { case (first, rest) => s"$first$rest" }

    val spannedId: P[Spanned[String]] = spanned(id)

    def spanned[A](parser: P[A]): P[Spanned[A]] =
      (P.index.with1 ~ parser ~ P.index).map { case (start -> parsed -> end) =>
        Spanned(parsed, Span(start, end))
      }

    def keyword(kw: String): P[Unit] = id.filter(kw == _) *> P.unit

    def inParens[A](parser: P[A]): P[A] =
      parser.between(P.char('(') *> ws, ws *> P.char(')'))
    def inParens0[A](parser: P0[A]): P0[A] =
      parser.between(P.char('(') *> ws, ws *> P.char(')'))
    def inBraces0[A](parser: P0[A]): P0[A] =
      parser.between(P.char('{') *> ws, ws *> P.char('}'))

    val expr: P[Expr] = P.defer(exprLazy <* ws)

    val intLiteral: P[Expr] = (digit.rep.string ~ P.index).map {
      case (num, end) => IntLiteral(num.toInt, Span(end - num.length, end))
    }
    val stringLiteral: P[StringLiteral] =
      spanned(
        ((P.char('\\') ~ P.anyChar) | (P.charWhere(_ != '"'))).rep.string
          .surroundedBy(P.char('"'))
      ).map { case Spanned(text, span) => StringLiteral(text, span) }
    val literal = intLiteral | stringLiteral
    val parenExpr = inParens(expr)
    val varRefOrFnCall =
      (P.index.with1 ~ id ~ inParens0(
        expr.repSep0(P.char(',') *> ws)
      ).? ~ P.index)
        .map {
          case (start -> name -> None -> end) =>
            VarRef(name, None, Span(start, end))
          case (start -> name -> Some(args) -> end) =>
            FnCall(
              Spanned(name, Span(start, start + name.length)),
              args,
              None,
              None,
              Span(start, end)
            )
        }
    val selectable: P[Expr] = varRefOrFnCall | parenExpr
    val fieldAccess: P[Expr] =
      (selectable ~ (ws.with1 *> P.char('.') *> ws *> spannedId).rep0).map {
        case (obj, fields) =>
          fields.foldLeft(obj) { (obj, field) => FieldAccess(obj, field, None) }
      }
    val binOp1 = binOp(fieldAccess, P.stringIn(List("*", "/")))
    val binOp2 = binOp(binOp1, P.stringIn(List("+", "-")))
    val binOp3 = binOp(binOp2, P.stringIn(List(">=", "<=", ">", "<", "=")))
    def binOp(prev: P[Expr], op: P[String]): P[Expr] =
      ((prev <* ws) ~ (op ~ (P.index <* ws) ~ prev <* ws).rep0).map {
        case (left, reps) =>
          reps.foldLeft(left) { case (lhs, op -> opEnd -> rhs) =>
            val binop = BinOp.values.find(_.text == op).get
            BinExpr(
              lhs,
              Spanned(binop, Span(opEnd - op.length, opEnd)),
              rhs,
              None
            )
          }
      }

    lazy val exprLazy: P[Expr] = binOp3

    val stmts: P[List[Stmt]] = P.defer(let | exprStmt)
    val let: P[List[Stmt]] =
      (spanned(
        (keyword("let") *> ws *> spannedId <* ws <* P.char('=') <* ws)
          ~ (expr <* ws <* P.char(';') <* ws)
      )
        ~ stmts).map { case (Spanned((name, expr), span), stmts) =>
        Stmt.VarDef(name, expr, span) :: stmts
      }
    val exprStmt: P[List[Stmt]] =
      (spanned(expr <* ws <* P.char(';')) ~ stmts).map {
        case (Spanned(expr, span), stmts) => Stmt.ExprStmt(expr, span) :: stmts
      }

    val block: P[Block] =
      spanned(P.char('{') *> ws *> stmts <* ws <* P.char('}')).map {
        case Spanned(stmts, span) =>
          Block(stmts, span)
      }

    val typeRef: P[TypeRef] = (P.index.with1 ~ id).map { case (start, name) =>
      TypeRef(name, Span(start, start + name.length))
    }

    val param: P[Param] =
      ((spannedId <* ws <* P.char(':') <* ws) ~ typeRef).map(Param(_, _))

    val fnDef: P[FnDef] = ((P.index.with1 <* keyword("fn") <* ws)
      ~ (spannedId <* ws <* P.char('(') <* ws)
      ~ ((param <* ws).repSep0(P.char(',') *> ws) <* ws <* P.char(')') <* ws)
      ~ (P.char(':') *> ws *> typeRef <* ws)
      ~ block).map { case (start -> name -> params -> returnType -> body) =>
      FnDef(name, params, returnType, body, Span(start, body.span.end))
    }

    val fieldDef: P[FieldDef] =
      ((P.index ~ (keyword("mut") *> ws).backtrack.?).with1
        ~ (spannedId <* ws <* P.char(':') <* ws)
        ~ typeRef)
        .map { case (start -> mutable -> name -> typ) =>
          FieldDef(mutable.isDefined, name, typ, Span(start, typ.span.end))
        }

    val enumCase: P[EnumCase] =
      ((spannedId <* ws)
        ~ inBraces0((fieldDef <* ws).repSep0(P.char(',') *> ws))
        ~ P.index <* ws).map { case (name -> fields -> end) =>
        EnumCase(name, fields, Span(name.span.start, end))
      }

    val enumDef: P[TypeDef] =
      (P.index.with1
        ~ (keyword("data") *> ws *> spannedId <* ws <* P.char('=') <* ws)
        ~ enumCase.repSep0(P.char('|') *> ws) ~ P.index <* ws).map {
        case (start -> name -> cases -> end) =>
          TypeDef(name, cases, Span(start, end))
      }

    val fileParser = (ws *> enumDef.rep0).map(
      ParsedFile(_, Nil)
    ) // (enumDef.rep0 ~ fnDef.rep0).map(ParsedFile(_, _))
  }
}
