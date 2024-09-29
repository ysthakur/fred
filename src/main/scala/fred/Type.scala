package fred

case class TypeRef(name: String, span: Span)

sealed trait Type {
  def name: String
  def span: Span
}

enum BuiltinType extends Type {
  case Int, Str

  override def name = {
    this match {
      case Int => "int"
      case Str => "str"
    }
  }

  override def span = Span.synthetic
}

case class TypeDef(nameSpanned: Spanned[String], cases: List[EnumCase], span: Span) extends Type {
  def name = nameSpanned.value
}

case class EnumCase(name: Spanned[String], fields: List[FieldDef], span: Span)

case class FieldDef(mutable: Boolean, name: Spanned[String], typ: TypeRef, span: Span)
