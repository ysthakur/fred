package fred

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

import org.scalacheck.Gen

object GenerateTypes {
  private val SomeField = "value"

  case class GenTypeAux(refs: List[Int])

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
  def genTypesAux(): Gen[List[GenTypeAux]] = {
    Gen.choose(1, 40).flatMap { numSccs =>
      Gen.sequence(0.until(numSccs).map { i =>
        Gen.listOf(Gen.chooseNum(0, i)).map(GenTypeAux(_))
      })
    }
  }

  private def nameForType(ind: Int) = s"T$ind"

  /** Name for the optional type corresponding to the type at index `ind` */
  private def nameForOptType(ind: Int) = s"OptT$ind"
  private def someCtorName(ind: Int) = s"Some$ind"
  private def noneCtorName(ind: Int) = s"None$ind"

  private def nameForField(ind: Int) = s"f$ind"

  def toTypes(typeAuxes: List[GenTypeAux]): List[TypeDef] = {
    val types = typeAuxes.zipWithIndex.map { case (GenTypeAux(refs), i) =>
      val typeName = Spanned(nameForType(i), Span.synth)
      TypeDef(
        typeName,
        List(
          EnumCase(
            typeName,
            refs.zipWithIndex.map { (typeInd, fieldInd) =>
              val fieldType =
                if (i == typeInd) nameForOptType(typeInd)
                else nameForType(typeInd)
              FieldDef(
                true,
                Spanned(nameForField(fieldInd), Span.synth),
                TypeRef(fieldType, Span.synth),
                Span.synth
              )
            },
            Span.synth
          )
        ),
        Span.synth
      )
    }
    // Generate optional types for self-referential types
    val optTypes = typeAuxes.zipWithIndex
      .filter { case (GenTypeAux(refs), i) =>
        refs.contains(i)
      }
      .map { (_, i) =>
        TypeDef(
          Spanned(nameForOptType(i), Span.synth),
          List(
            EnumCase(
              Spanned(someCtorName(i), Span.synth),
              List(
                FieldDef(
                  false,
                  Spanned(SomeField, Span.synth),
                  TypeRef(nameForType(i), Span.synth),
                  Span.synth
                )
              ),
              Span.synth
            ),
            EnumCase(
              Spanned(noneCtorName(i), Span.synth),
              Nil,
              Span.synth
            )
          ),
          Span.synth
        )
      }
    types ++ optTypes
  }

  def genCode(typeAuxes: List[GenTypeAux]): Gen[ParsedFile] = {
    val types = toTypes(typeAuxes)

    def genExpr(typeInd: Int): Gen[Expr] = {
      val GenTypeAux(refs) = typeAuxes(typeInd)
      val fieldGens = refs.zipWithIndex.map { case (fieldTypeInd, fieldInd) =>
        val fieldName = Spanned(nameForField(fieldInd), Span.synth)
        val value =
          if (fieldTypeInd < typeInd) {
            genExpr(fieldTypeInd)
          } else {
            Gen.const(
              CtorCall(
                Spanned(noneCtorName(typeInd), Span.synth),
                Nil,
                Span.synth
              )
            )
          }
        value.map((fieldName, _))
      }
      // TODO Why in the world is this an ArrayList?
      Gen.sequence(fieldGens).map { fields =>
        CtorCall(
          Spanned(nameForType(typeInd), Span.synth),
          fields.asScala.toList,
          Span.synth
        )
      }
    }

    genExpr(typeAuxes.length - 1).map { expr =>
      ParsedFile(
        types,
        List(
          FnDef(
            Spanned("main", Span.synth),
            Nil,
            TypeRef("int", Span.synth),
            LetExpr(
              Spanned("ignored", Span.synth),
              expr,
              IntLiteral(0, Span.synth),
              Span.synth
            ),
            Span.synth
          )
        )
      )
    }
  }
}
