package fred

import scala.collection.mutable

import org.scalacheck.Gen

object GenerateTypes {
  case class GeneratedType(refs: List[Int])

  /** Generate a bunch of types. Each type is its own strongly-connected
    * component. The returned list is sorted backwards (types that come later in
    * the list can have references to types that come earlier in the list).
    *
    * Types can have references to themselves (so we can test cycles). To allow
    * this to work without problems, self-references must go through an Option
    * type generated for that type.
    *
    * All references are mutable, for testing cycles.
    */
  def genTypes(): Gen[List[GeneratedType]] = {
    Gen.choose(1, 40).flatMap { numSccs =>
      Gen.sequence(0.until(numSccs).map { i =>
        Gen.listOf(Gen.chooseNum(0, i)).map(GeneratedType(_))
      })
    }
  }

  def generateAst(typeAuxes: List[GeneratedType]): ParsedFile = {
    def nameFor(ind: Int) = s"T$ind"

    /** Name for the optional type corresponding to the type at index `ind` */
    def nameForOpt(ind: Int) = s"OptT$ind"

    val types = typeAuxes.zipWithIndex.map { case (GeneratedType(refs), i) =>
      val typeName = Spanned(nameFor(i), Span.synthetic)
      TypeDef(
        typeName,
        List(
          EnumCase(
            typeName,
            refs.zipWithIndex.map { (typeInd, fieldInd) =>
              val fieldType =
                if (i == typeInd) nameForOpt(typeInd) else nameFor(typeInd)
              FieldDef(
                true,
                Spanned(s"f$fieldInd", Span.synthetic),
                TypeRef(fieldType, Span.synthetic),
                Span.synthetic
              )
            },
            Span.synthetic
          )
        ),
        Span.synthetic
      )
    }
    // Generate optional types for self-referential types
    val optTypes = typeAuxes.zipWithIndex
      .filter { case (GeneratedType(refs), i) =>
        refs.contains(i)
      }
      .map { (_, i) =>
        val name = Spanned(nameForOpt(i), Span.synthetic)
        TypeDef(
          name,
          List(
            EnumCase(
              name,
              List(
                FieldDef(
                  false,
                  Spanned("value", Span.synthetic),
                  TypeRef(nameFor(i), Span.synthetic),
                  Span.synthetic
                )
              ),
              Span.synthetic
            )
          ),
          Span.synthetic
        )
      }
    ParsedFile(types ++ optTypes, Nil)
  }
}
