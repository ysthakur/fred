package fred

import scala.util.Random

import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen
import org.scalatest.tagobjects.Slow
import org.scalacheck.Shrink

import fred.Compiler.Settings

class FuzzTests
    extends AnyPropSpec with ScalaCheckPropertyChecks with should.Matchers {

  property("Simple generated programs") {
    given PropertyCheckConfiguration =
      PropertyCheckConfiguration(minSize = 1, sizeRange = 10)
    forAll(GenUtil.genTypesAux().flatMap(GenUtil.genCode)) { parsedFile =>
      ExecTests.valgrindCheck(
        parsedFile,
        None,
        None,
        save = Some("blech-ijaksd.c"),
        settings = Settings(includeMemcheck = true)
      )
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
      assignExprs <- GenUtil
        .genAssignments(allTypes, allVars.view.mapValues(_.keySet).toMap)
    } yield {
      val finalRes = IntLiteral(0, Span.synth)
      val withAssignments = assignExprs.foldRight(finalRes: Expr)(
        BinExpr(_, Spanned(BinOp.Seq, Span.synth), _)
      )
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
      PropertyCheckConfiguration(minSize = 1, sizeRange = 30)

    /** Insert a call to decrement the reference count of every variable after
      * its last usage
      * @param varTypes
      *   Map variable names to types
      * @return
      */
    def insertDecrs(
        assignExprs: List[SetFieldExpr],
        allVars: Iterable[String],
        varTypes: Map[String, String]
    ): List[Expr] = {
      def decrVar(varName: String): Expr = FnCall(
        Spanned("c", Span.synth),
        List(
          // Why the "; " at the start? So it's not matched in the regex at the bottom
          // of this test and removed
          StringLiteral(s"; $$decr_${varTypes(varName)}($varName);", Span.synth)
        ),
        None,
        None,
        Span.synth
      )

      def rec(assignExprs: List[SetFieldExpr]): (List[Expr], Set[String]) = {
        assignExprs match {
          case Nil => (Nil, allVars.toSet)
          case (expr @ SetFieldExpr(
                Spanned(lhsVarName, _),
                _,
                CtorCall(_, List((_, VarRef(rhsVarName, _))), _),
                span
              )) :: rest =>
            val (next, remVars) = rec(rest)
            val decrLhs =
              if (remVars.contains(lhsVarName)) List(decrVar(lhsVarName))
              else Nil
            val decrRhs =
              if (remVars.contains(rhsVarName)) List(decrVar(rhsVarName))
              else Nil
            (
              expr :: decrLhs ::: decrRhs ::: next,
              remVars - lhsVarName - rhsVarName
            )
          case expr :: _ =>
            throw new Error(s"[insertDecrs.rec] Unexpected kind of expr: $expr")
        }
      }
      val (res, remVars) = rec(assignExprs)
      remVars.map(decrVar).toList ::: res
    }

    // TODO maybe randomness isn't needed here, just insert one check every few statements or something
    def insertValgrindChecks(stmts: List[Expr]): Gen[List[Expr]] = {
      GenUtil.sequence(stmts.map { stmt =>
        Gen.prob(0.1).map { insert =>
          if (insert) {
            List(
              stmt,
              FnCall(
                Spanned("c", Span.synth),
                List(StringLiteral(
                  "processAllPCRs(); VALGRIND_DO_CHANGED_LEAK_CHECK;",
                  Span.synth
                )),
                None,
                None,
                Span.synth
              )
            )
          } else { List(stmt) }
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
      withDecrs = insertDecrs(
        assignExprs,
        allVars.values.flatMap(_.keySet),
        allVars.flatMap((typ, vars) => vars.keySet.map(_ -> typ)).toMap
      )
      withChecks <- insertValgrindChecks(withDecrs)
    } yield {
      val finalRes = IntLiteral(0, Span.synth)
      val withAssignments = withChecks.foldRight(finalRes: Expr)(
        BinExpr(_, Spanned(BinOp.Seq, Span.synth), _)
      )
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
