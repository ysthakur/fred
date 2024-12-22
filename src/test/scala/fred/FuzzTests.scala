package fred

import scala.util.Random

import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen
import org.scalatest.tagobjects.Slow
import org.scalacheck.Shrink

import fred.Compiler.Settings
import fred.GenUtil.{GenStmt, GeneratedProgram}

class FuzzTests
    extends AnyPropSpec with ScalaCheckPropertyChecks with should.Matchers {
  property("No intermediate checks", Slow) {
    given PropertyCheckConfiguration =
      PropertyCheckConfiguration(minSize = 5, sizeRange = 30)

    val genFull = for {
      size <- Gen.size
      numTypes <- Gen.choose(1, math.sqrt(size).toInt)
      rawTypes <- GenUtil.genTypesFullRandom(numTypes)
      allTypes = GenUtil.addOptTypes(rawTypes)
      sccs = Cycles.fromFile(ParsedFile(allTypes, Nil)).sccs
      allVars <- GenUtil.genVars(sccs, size / numTypes)
      assignExprs <- GenUtil
        .genAssignments(allTypes, allVars.view.mapValues(_.keySet).toMap)
    } yield {
      val finalRes = IntLiteral(0, Span.synth)
      val withAssignments = assignExprs.foldRight(finalRes: Expr) {
        (stmt, acc) => BinExpr(stmt.asAst, Spanned(BinOp.Seq, Span.synth), acc)
      }
      val withVarDefs = sccs.flatMap(
        _.filterNot(_.name.startsWith("Opt")).flatMap(typ => allVars(typ.name))
      ).foldLeft(withAssignments) { case (body, (varName, value)) =>
        LetExpr(Spanned(varName, Span.synth), value, body, Span.synth)
      }
      ParsedFile(
        allTypes,
        List(FnDef(
          Spanned("main", Span.synth),
          Nil,
          TypeRef("int", Span.synth),
          withVarDefs,
          Span.synth
        ))
      )
    }

    forAll(genFull) { parsedFile =>
      ExecTests.valgrindCheck(
        parsedFile,
        None,
        None,
        save = Some("blech-oiwjsd.c"),
        settings = Settings(includeMemcheck = true)
      )
    }
  }

  property("With intermediate checks", Slow) {
    given PropertyCheckConfiguration =
      PropertyCheckConfiguration(minSize = 5, sizeRange = 60)
    given Shrink[GeneratedProgram] = GenUtil.shrinkGenerated

    // TODO maybe randomness isn't needed here, just insert one check every few statements or something
    def insertValgrindChecks(stmts: List[GenStmt]): Gen[List[GenStmt]] = {
      GenUtil.sequence(stmts.map { stmt =>
        Gen.prob(0.1).map { insert =>
          if (insert) List(stmt, GenStmt.ValgrindCheck) else List(stmt)
        }
      }).map(_.flatten)
    }

    val genFull = for {
      size <- Gen.size
      numTypes <- Gen.choose(1, math.sqrt(size).toInt)
      rawTypes <- GenUtil.genTypesFullRandom(numTypes)
      allTypes = GenUtil.addOptTypes(rawTypes)
      sccs = Cycles.fromFile(ParsedFile(allTypes, Nil)).sccs
      allVars <- GenUtil.genVars(sccs, size / numTypes)
      assignExprsOrdered <- GenUtil
        .genAssignments(allTypes, allVars.view.mapValues(_.keySet).toMap)
      seed <- Gen.long
      rand = Random(seed)
      assignExprs = Random.shuffle(assignExprsOrdered)
      withChecks <- insertValgrindChecks(assignExprs)
    } yield {
      GeneratedProgram(
        allTypes,
        allVars.flatMap((typ, vars) => vars.keySet.map(_ -> typ)).toMap,
        sccs.flatMap(
          _.filterNot(_.name.startsWith("Opt"))
            .flatMap(typ => allVars(typ.name))
        ),
        withChecks
      )
    }

    forAll(genFull) {
      generated =>
        ExecTests.valgrindCheck(
          generated.asAst,
          None,
          None,
          save = Some("blech-1234sd.c"),
          settings = Settings(includeMemcheck = true),
          processC = c => {
            // TODO make lexical lifetimes or whatever they're called a setting in
            // the compiler instead of this buffoonery
            // Remove all $decr_TX calls for the variables at the end (but not $decr_OptTX calls)
            // because we're calling $decr_TX ourselves above
            val (beforeMain, afterMain) = c.splitAt(c.indexOf("int main"))
            beforeMain +
              afterMain.replaceAll("""  \$decr_T\d+\(vT\d+_\d+\);\n""", "")
          }
        )
    }
  }
}
