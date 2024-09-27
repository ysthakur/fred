package fred

case class TypeRef(name: String, span: Span)

sealed trait Type {
  def span: Span
}

enum BuiltinType extends Type {
  case Int, Str

  override def span = Span.synthetic
}

case class TypeDef(name: Spanned[String], cases: List[EnumCase], span: Span) extends Type

case class EnumCase(name: Spanned[String], fields: List[FieldDef], span: Span)

case class FieldDef(mutable: Boolean, name: Spanned[String], typ: TypeRef, span: Span)
