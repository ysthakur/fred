package fred

import scala.util.Random

import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen
import org.scalatest.tagobjects.Slow
import org.scalacheck.Shrink
import org.scalacheck.Gen.Parameters
import org.scalacheck.rng.Seed

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

  val gen = GeneratedProgram(
    List(
      TypeDef(
        Spanned("OptT0", Span.synth),
        List(
          EnumCase(
            Spanned("SomeT0", Span.synth),
            List(FieldDef(false, Spanned("value", Span.synth), TypeRef("T0", Span.synth), Span.synth)),
            Span.synth
          ),
          EnumCase(Spanned("NoneT0", Span.synth), List(), Span.synth)
        ),
        Span.synth
      ),
      TypeDef(
        Spanned("OptT1", Span.synth),
        List(
          EnumCase(
            Spanned("SomeT1", Span.synth),
            List(FieldDef(false, Spanned("value", Span.synth), TypeRef("T1", Span.synth), Span.synth)),
            Span.synth
          ),
          EnumCase(Spanned("NoneT1", Span.synth), List(), Span.synth)
        ),
        Span.synth
      ),
      TypeDef(
        Spanned("OptT2", Span.synth),
        List(
          EnumCase(
            Spanned("SomeT2", Span.synth),
            List(FieldDef(false, Spanned("value", Span.synth), TypeRef("T2", Span.synth), Span.synth)),
            Span.synth
          ),
          EnumCase(Spanned("NoneT2", Span.synth), List(), Span.synth)
        ),
        Span.synth
      ),
      TypeDef(
        Spanned("T2", Span.synth),
        List(EnumCase(
          Spanned("T2", Span.synth),
          List(
            FieldDef(false, Spanned("f3", Span.synth), TypeRef("T1", Span.synth), Span.synth),
            FieldDef(true, Spanned("f1", Span.synth), TypeRef("OptT2", Span.synth), Span.synth),
            FieldDef(true, Spanned("f6", Span.synth), TypeRef("OptT2", Span.synth), Span.synth),
            FieldDef(true, Spanned("f10", Span.synth), TypeRef("OptT2", Span.synth), Span.synth),
            FieldDef(true, Spanned("f4", Span.synth), TypeRef("OptT0", Span.synth), Span.synth),
            FieldDef(true, Spanned("f2", Span.synth), TypeRef("OptT2", Span.synth), Span.synth),
            FieldDef(false, Spanned("f0", Span.synth), TypeRef("T1", Span.synth), Span.synth),
            FieldDef(true, Spanned("f11", Span.synth), TypeRef("OptT2", Span.synth), Span.synth),
            FieldDef(true, Spanned("f7", Span.synth), TypeRef("OptT0", Span.synth), Span.synth),
            FieldDef(true, Spanned("f8", Span.synth), TypeRef("OptT0", Span.synth), Span.synth),
            FieldDef(true, Spanned("f9", Span.synth), TypeRef("OptT2", Span.synth), Span.synth),
            FieldDef(true, Spanned("f5", Span.synth), TypeRef("OptT2", Span.synth), Span.synth)
          ),
          Span.synth
        )),
        Span.synth
      ),
      TypeDef(
        Spanned("T0", Span.synth),
        List(EnumCase(
          Spanned("T0", Span.synth),
          List(
            FieldDef(true, Spanned("f0", Span.synth), TypeRef("OptT0", Span.synth), Span.synth),
            FieldDef(true, Spanned("f1", Span.synth), TypeRef("OptT2", Span.synth), Span.synth)
          ),
          Span.synth
        )),
        Span.synth
      ),
      TypeDef(
        Spanned("T1", Span.synth),
        List(EnumCase(
          Spanned("T1", Span.synth),
          List(FieldDef(true, Spanned("f0", Span.synth), TypeRef("OptT1", Span.synth), Span.synth)),
          Span.synth
        )),
        Span.synth
      )
    ),
    Map(
      "vT2_1" -> "T2",
      "vT1_1" -> "T1",
      "vT0_3" -> "T0",
      "vT0_0" -> "T0",
      "vT0_1" -> "T0",
      "vT1_3" -> "T1",
      "vT2_0" -> "T2",
      "vT0_2" -> "T0",
      "vT2_2" -> "T2",
      "vT1_0" -> "T1",
      "vT1_2" -> "T1",
      "vT2_3" -> "T2"
    ),
    List(
      (
        "vT0_3",
        CtorCall(
          Spanned("T0", Span.synth),
          List(
            (Spanned("f0", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f1", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth))
          ),
          Span.synth
        )
      ),
      (
        "vT2_1",
        CtorCall(
          Spanned("T2", Span.synth),
          List(
            (Spanned("f3", Span.synth), VarRef("vT1_2", Span.synth)),
            (Spanned("f1", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f6", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f10", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f4", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f2", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f0", Span.synth), VarRef("vT1_2", Span.synth)),
            (Spanned("f11", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f7", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f8", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f9", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f5", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth))
          ),
          Span.synth
        )
      ),
      (
        "vT0_1",
        CtorCall(
          Spanned("T0", Span.synth),
          List(
            (Spanned("f0", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f1", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth))
          ),
          Span.synth
        )
      ),
      (
        "vT2_3",
        CtorCall(
          Spanned("T2", Span.synth),
          List(
            (Spanned("f3", Span.synth), VarRef("vT1_0", Span.synth)),
            (Spanned("f1", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f6", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f10", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f4", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f2", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f0", Span.synth), VarRef("vT1_1", Span.synth)),
            (Spanned("f11", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f7", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f8", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f9", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f5", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth))
          ),
          Span.synth
        )
      ),
      (
        "vT2_0",
        CtorCall(
          Spanned("T2", Span.synth),
          List(
            (Spanned("f3", Span.synth), VarRef("vT1_3", Span.synth)),
            (Spanned("f1", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f6", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f10", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f4", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f2", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f0", Span.synth), VarRef("vT1_2", Span.synth)),
            (Spanned("f11", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f7", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f8", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f9", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f5", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth))
          ),
          Span.synth
        )
      ),
      (
        "vT2_2",
        CtorCall(
          Spanned("T2", Span.synth),
          List(
            (Spanned("f3", Span.synth), VarRef("vT1_2", Span.synth)),
            (Spanned("f1", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f6", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f10", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f4", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f2", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f0", Span.synth), VarRef("vT1_1", Span.synth)),
            (Spanned("f11", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f7", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f8", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f9", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth)),
            (Spanned("f5", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth))
          ),
          Span.synth
        )
      ),
      (
        "vT0_0",
        CtorCall(
          Spanned("T0", Span.synth),
          List(
            (Spanned("f0", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f1", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth))
          ),
          Span.synth
        )
      ),
      (
        "vT0_2",
        CtorCall(
          Spanned("T0", Span.synth),
          List(
            (Spanned("f0", Span.synth), CtorCall(Spanned("NoneT0", Span.synth), List(), Span.synth)),
            (Spanned("f1", Span.synth), CtorCall(Spanned("NoneT2", Span.synth), List(), Span.synth))
          ),
          Span.synth
        )
      ),
      (
        "vT1_0",
        CtorCall(
          Spanned("T1", Span.synth),
          List((Spanned("f0", Span.synth), CtorCall(Spanned("NoneT1", Span.synth), List(), Span.synth))),
          Span.synth
        )
      ),
      (
        "vT1_1",
        CtorCall(
          Spanned("T1", Span.synth),
          List((Spanned("f0", Span.synth), CtorCall(Spanned("NoneT1", Span.synth), List(), Span.synth))),
          Span.synth
        )
      ),
      (
        "vT1_2",
        CtorCall(
          Spanned("T1", Span.synth),
          List((Spanned("f0", Span.synth), CtorCall(Spanned("NoneT1", Span.synth), List(), Span.synth))),
          Span.synth
        )
      ),
      (
        "vT1_3",
        CtorCall(
          Spanned("T1", Span.synth),
          List((Spanned("f0", Span.synth), CtorCall(Spanned("NoneT1", Span.synth), List(), Span.synth))),
          Span.synth
        )
      )
    ),
    List(
      GenStmt.Assign("vT0_3", "f0", "T0", "vT0_1"),
      GenStmt.Assign("vT0_1", "f0", "T0", "vT0_1"),
      GenStmt.ValgrindCheck,
      GenStmt.Assign("vT0_0", "f0", "T0", "vT0_3")
    )
  )

  property("With intermediate checks", Slow) {
    given PropertyCheckConfiguration =
      PropertyCheckConfiguration(minSize = 5, sizeRange = 40)
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

    //forAll(genFull.apply(Parameters.default, Seed(6529581269390504088L)).get) {
    forAll(Gen.const(gen)) {
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
