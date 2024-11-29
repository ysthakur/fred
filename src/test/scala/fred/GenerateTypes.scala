package fred

import scala.jdk.CollectionConverters.*

import org.scalacheck.Gen
import org.scalacheck.Shrink

object GenerateTypes {
  val SomeField = "value"

  given noShrink[T]: Shrink[T] = Shrink(_ => Stream.empty)

  case class GenTypeAux(refs: List[Int])

  def sequence[T](gens: Iterable[Gen[T]]): Gen[List[T]] =
    Gen.sequence[List[T], T](gens)

  /** Generate a bunch of types completely randomly (any type could have
    * references to any other type). All references are mutable.
    */
  def genTypesFullRandom(numTypes: Int): Gen[List[TypeDef]] = {
    sequence(0.until(numTypes).map { i =>
      Gen.listOf(Gen.choose(0, numTypes - 1))
    }).map { types =>
      val graph = types.zipWithIndex.map { (refs, typeInd) =>
        s"T$typeInd" -> refs.zipWithIndex
          .map((ref, i) => (true, s"f$i", s"T$ref"))
          .toSet
      }.toMap
      toTypeDefs(graph)
    }
  }

  /** Create an optional type OptT for every given type, and update the given
    * types so that any reference to a type T in the same SCC gets replaced with
    * a mutable reference to the optional type OptT
    * @return
    *   The optional types and updated types all concatenated into a single list
    */
  def addOptTypes(types: List[TypeDef]): List[TypeDef] = {
    val cycles = Cycles.fromFile(ParsedFile(types, Nil))
    val safeTypes = cycles.sccs.flatMap { scc =>
      scc.map { typ =>
        typ.copy(cases = typ.cases.map { variant =>
          variant.copy(fields = variant.fields.map { field =>
            if (scc.exists(_.name == field.typ.name))
              field.copy(
                mutable = true,
                typ = TypeRef(s"Opt${field.typ.name}", Span.synth)
              )
            else field.copy(mutable = false, span = Span(-4, -6))
          })
        })
      }
    }

    val optTypes = types.map { typ =>
      TypeDef(
        Spanned(s"Opt${typ.name}", Span.synth),
        List(
          EnumCase(
            Spanned(s"Some${typ.name}", Span.synth),
            List(
              FieldDef(
                false,
                Spanned(SomeField, Span.synth),
                TypeRef(typ.name, Span.synth),
                Span.synth
              )
            ),
            Span.synth
          ),
          EnumCase(
            Spanned(s"None${typ.name}", Span.synth),
            Nil,
            Span.synth
          )
        ),
        Span.synth
      )
    }

    optTypes ::: safeTypes
  }

  /** Generate a bunch of types. Each type is its own strongly-connected
    * component. Types that come later in the list can have references to types
    * that come earlier in the list, as well as themselves.
    *
    * Types can have references to themselves (so we can test cycles). To allow
    * this to work without problems, self-references must go through an Option
    * type generated for that type.
    *
    * All references are mutable, for testing cycles.
    */
  def genTypesAux(): Gen[List[GenTypeAux]] = {
    Gen.sized { numSccs =>
      sequence(0.until(numSccs).map { i =>
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

  /** Turn a graph of TypeDef names into a list of TypeDefs
    *
    * @param graph
    *   Maps TypeDef names to their fields. Fields are `(mutable, fieldName,
    *   fieldTypeName)`
    * @return
    */
  def toTypeDefs(
      graph: Map[String, Set[(Boolean, String, String)]]
  ): List[TypeDef] = {
    graph.map { (name, _) =>
      val spannedName = Spanned(name, Span.synth)
      val fields = graph(name).toList.map {
        (mutable, fieldName, neighborName) =>
          FieldDef(
            mutable,
            Spanned(fieldName, Span.synth),
            TypeRef(neighborName, Span.synth),
            Span.synth
          )
      }
      TypeDef(
        spannedName,
        List(
          EnumCase(spannedName, fields, Span.synth)
        ),
        Span.synth
      )
    }.toList
  }

  def toTypesWithOpts(typeAuxes: List[GenTypeAux]): List[TypeDef] = {
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
    val types = toTypesWithOpts(typeAuxes)

    def genExpr(typeInd: Int): Gen[Expr] = {
      val GenTypeAux(refs) = typeAuxes(typeInd)
      // todo create multiple objects and create cycles between them
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
      sequence(fieldGens).map { fields =>
        CtorCall(
          Spanned(nameForType(typeInd), Span.synth),
          fields,
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
            BinExpr(
              expr,
              Spanned(BinOp.Seq, Span.synth),
              IntLiteral(0, Span.synth)
            ),
            Span.synth
          )
        )
      )
    }
  }

  /** Generate a tree of types. Each type `T$typeNum` will have some number of
    * references to itself, but indirectly, through an `OptT$typeNum` type. That
    * way, you can reassign the field holding the optional value to create
    * cycles.
    *
    * @param maxSelfRefs
    *   How many references to itself each type should have at most
    * @return
    *   The first element is the actual type, while the second element is all
    *   the types it refers to, as well as the optional types corresponding to
    *   each type.
    */
  def genTypeTree(maxSelfRefs: Int): Gen[(TypeDef, List[TypeDef])] = {
    var typeNum = 0
    def helper(): Gen[(TypeDef, List[TypeDef])] = {
      for {
        size <- Gen.size
        numSelfRefs <- Gen.choose(0, maxSelfRefs)
        numOtherRefs <- Gen.geometric(1.0)
        otherTypes <- Gen.listOfN(numOtherRefs, helper())
      } yield {
        typeNum += 1
        val (fieldTypes, rest) = otherTypes.unzip
        val typeName = s"T$typeNum"
        val optTypeName = s"OptT$typeNum"
        val selfRefFields = 1.to(numSelfRefs).map { fieldInd =>
          FieldDef(
            true,
            Spanned(s"self$fieldInd", Span.synth),
            TypeRef(optTypeName, Span.synth),
            Span.synth
          )
        }
        val otherRefFields = fieldTypes.zipWithIndex.map { (typ, fieldInd) =>
          FieldDef(
            true,
            Spanned(s"f$fieldInd", Span.synth),
            TypeRef(typ.name, Span.synth),
            Span.synth
          )
        }
        val thisType = TypeDef(
          Spanned(typeName, Span.synth),
          List(
            EnumCase(
              Spanned(typeName, Span.synth),
              selfRefFields ++ otherRefFields,
              Span.synth
            )
          ),
          Span.synth
        )
        val optType = TypeDef(
          Spanned(optTypeName, Span.synth),
          List(
            EnumCase(
              Spanned(s"Some$typeName", Span.synth),
              List(
                FieldDef(
                  false,
                  Spanned("value", Span.synth),
                  TypeRef(typeName, Span.synth),
                  Span.synth
                )
              ),
              Span.synth
            ),
            EnumCase(
              Spanned(s"None$typeName", Span.synth),
              Nil,
              Span.synth
            )
          ),
          Span.synth
        )
        (thisType, optType :: fieldTypes ::: rest.flatten)
      }
    }

    helper()
  }
}
