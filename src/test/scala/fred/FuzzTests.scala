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
    forAll(GenUtil.genTypesAux().flatMap(GenUtil.genCode)) { parsedFile =>
      ExecTests.valgrindCheck(parsedFile, None, None)
    }
  }

  property("No intermediate checks", Slow) {
    given PropertyCheckConfiguration =
      PropertyCheckConfiguration(minSize = 1, sizeRange = 30)

    val genFull = for {
      size <- Gen.size
      numTypes <- Gen.choose(1, math.sqrt(size).toInt)
      rawTypes <- GenUtil.genTypesFullRandom(numTypes)
      allTypes = GenUtil.addOptTypes(rawTypes)
      sccs = Cycles.fromFile(ParsedFile(allTypes, Nil)).sccs
      allVars <- GenUtil.genVars(sccs, size / numTypes)
      assignExprs <- GenUtil.genAssignments(
        allTypes,
        allVars.view.mapValues(_.keySet).toMap
      )
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
