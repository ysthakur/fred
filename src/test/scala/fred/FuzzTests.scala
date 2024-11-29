package fred

import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen
import org.scalatest.tagobjects.Slow
import org.scalacheck.Shrink

class FuzzTests
    extends AnyPropSpec
    with ScalaCheckPropertyChecks
    with should.Matchers {

  property("Simple generated programs") {
    given PropertyCheckConfiguration =
      PropertyCheckConfiguration(minSize = 1, sizeRange = 10)
    forAll(GenerateTypes.genTypesAux().flatMap(GenerateTypes.genCode)) {
      parsedFile => ExecTests.valgrindCheck(parsedFile, None, None)
    }
  }

  property("Generated programs", Slow) {
    given PropertyCheckConfiguration =
      PropertyCheckConfiguration(minSize = 1, sizeRange = 30)

    given [T]: Shrink[T] = Shrink(_ => Stream.empty)

    def genVars(
        typ: TypeDef,
        prevTypes: Map[String, Map[String, Expr]]
    ): Gen[Map[String, Expr]] = {
      for {
        size <- Gen.size
        numVars <- Gen.choose(1, math.sqrt(size).ceil.toInt)
        values <- GenerateTypes.sequence(1.to(numVars).map { i =>
          GenerateTypes
            .sequence(typ.cases.head.fields.map { field =>
              prevTypes.get(field.typ.name) match {
                case Some(vars) =>
                  Gen
                    .oneOf(vars.keySet)
                    .map(field.name -> VarRef(_, None, Span.synth))
                case None =>
                  Gen.const(
                    field.name -> CtorCall(
                      Spanned(
                        s"None${field.typ.name.stripPrefix("Opt")}",
                        Span.synth
                      ),
                      Nil,
                      Span.synth
                    )
                  )
              }
            })
            .map { fieldArgs =>
              CtorCall(typ.cases.head.name, fieldArgs, Span.synth)
            }
        })
      } yield values.zipWithIndex.map { (value, i) =>
        s"v${typ.name}_$i" -> value
      }.toMap
    }

    /** @return A list of expressions that assign objects to create cycles */
    def createCycles(
        typ: TypeDef,
        allVars: Map[String, Set[String]]
    ): Gen[List[Expr]] = {
      val currVars = allVars(typ.name)
      GenerateTypes
        .sequence(
          typ.cases.head.fields.filter(_.typ.name.startsWith("Opt")).map {
            field =>
              val fieldType = field.typ.name.stripPrefix("Opt")
              val candidates = allVars
                .getOrElse(
                  fieldType,
                  throw new RuntimeException(
                    s"[createCycles] No such type: $fieldType"
                  )
                )
              Gen.someOf(currVars).flatMap { vars =>
                GenerateTypes.sequence(vars.map { varName =>
                  Gen.oneOf(candidates).map { ref =>
                    SetFieldExpr(
                      Spanned(varName, Span.synth),
                      field.name,
                      CtorCall(
                        Spanned(s"Some$fieldType", Span.synth),
                        List(
                          (
                            Spanned(GenerateTypes.SomeField, Span.synth),
                            VarRef(ref, None, Span.synth)
                          )
                        ),
                        Span.synth
                      ),
                      Span.synth
                    )
                  }
                })
              }
          }
        )
        .map(_.flatten)
    }

    val genFull = for {
      size <- Gen.size
      rawTypes <- GenerateTypes.genTypesFullRandom(math.sqrt(size).ceil.toInt)
      allTypes = GenerateTypes.addOptTypes(rawTypes)
      sccs = Cycles.fromFile(ParsedFile(allTypes, Nil)).sccs
      allVars <- sccs.foldRight(
        Gen.const(Map.empty[String, Map[String, Expr]])
      ) { (scc, prevVars) =>
        prevVars.flatMap { prevVars =>
          GenerateTypes
            .sequence(
              scc
                .filterNot(_.name.startsWith("Opt"))
                .map(typ => genVars(typ, prevVars).map(typ.name -> _))
            )
            .map(_.toMap ++ prevVars)
        }
      }
      assignExprs <- GenerateTypes
        .sequence(
          allTypes.filterNot(_.name.startsWith("Opt")).map { typ =>
            createCycles(typ, allVars.mapValues(_.keySet).toMap)
          }
        )
        .map(_.flatten)
    } yield {
      val finalRes = IntLiteral(0, Span.synth)
      val withAssignments = assignExprs.foldRight(finalRes: Expr)(
        BinExpr(_, Spanned(BinOp.Seq, Span.synth), _)
      )
      val withVarDefs = sccs
        .flatMap(
          _.filterNot(_.name.startsWith("Opt")).flatMap(typ =>
            allVars(typ.name)
          )
        )
        .foldLeft(withAssignments) { case (body, (varName, value)) =>
          LetExpr(Spanned(varName, Span.synth), value, body, Span.synth)
        }
      ParsedFile(
        allTypes,
        List(
          FnDef(
            Spanned("main", Span.synth),
            Nil,
            TypeRef("int", Span.synth),
            withVarDefs,
            Span.synth
          )
        )
      )
    }

    forAll(genFull) { parsedFile =>
      ExecTests.valgrindCheck(parsedFile, None, None, print = true)
    }
  }
}
