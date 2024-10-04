package fred

case class CompileError(msg: String, span: Span) extends RuntimeException(msg)
