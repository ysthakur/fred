package fred

import scala.util.Random
import java.nio.file.Files
import java.nio.file.Path

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

  val bad = GeneratedProgram(
    List(
      TypeDef(
        Spanned("OptT0", Span(-1, -1)),
        List(
          EnumCase(
            Spanned("SomeT0", Span(-1, -1)),
            List(FieldDef(
              false,
              Spanned("value", Span(-1, -1)),
              TypeRef("T0", Span(-1, -1)),
              Span(-1, -1)
            )),
            Span(-1, -1)
          ),
          EnumCase(Spanned("NoneT0", Span(-1, -1)), List(), Span(-1, -1))
        ),
        Span(-1, -1)
      ),
      TypeDef(
        Spanned("OptT1", Span(-1, -1)),
        List(
          EnumCase(
            Spanned("SomeT1", Span(-1, -1)),
            List(FieldDef(
              false,
              Spanned("value", Span(-1, -1)),
              TypeRef("T1", Span(-1, -1)),
              Span(-1, -1)
            )),
            Span(-1, -1)
          ),
          EnumCase(Spanned("NoneT1", Span(-1, -1)), List(), Span(-1, -1))
        ),
        Span(-1, -1)
      ),
      TypeDef(
        Spanned("T1", Span(-1, -1)),
        List(EnumCase(
          Spanned("T1", Span(-1, -1)),
          List(
            FieldDef(
              true,
              Spanned("f0", Span(-1, -1)),
              TypeRef("OptT1", Span(-1, -1)),
              Span(-1, -1)
            ),
            FieldDef(
              true,
              Spanned("f1", Span(-1, -1)),
              TypeRef("OptT0", Span(-1, -1)),
              Span(-1, -1)
            ),
            FieldDef(
              true,
              Spanned("f2", Span(-1, -1)),
              TypeRef("OptT1", Span(-1, -1)),
              Span(-1, -1)
            ),
            FieldDef(
              true,
              Spanned("f3", Span(-1, -1)),
              TypeRef("OptT1", Span(-1, -1)),
              Span(-1, -1)
            )
          ),
          Span(-1, -1)
        )),
        Span(-1, -1)
      ),
      TypeDef(
        Spanned("T0", Span(-1, -1)),
        List(EnumCase(
          Spanned("T0", Span(-1, -1)),
          List(
            FieldDef(
              true,
              Spanned("f0", Span(-1, -1)),
              TypeRef("OptT0", Span(-1, -1)),
              Span(-1, -1)
            ),
            FieldDef(
              true,
              Spanned("f1", Span(-1, -1)),
              TypeRef("OptT1", Span(-1, -1)),
              Span(-1, -1)
            ),
            FieldDef(
              true,
              Spanned("f2", Span(-1, -1)),
              TypeRef("OptT0", Span(-1, -1)),
              Span(-1, -1)
            ),
            FieldDef(
              true,
              Spanned("f3", Span(-1, -1)),
              TypeRef("OptT1", Span(-1, -1)),
              Span(-1, -1)
            )
          ),
          Span(-1, -1)
        )),
        Span(-1, -1)
      )
    ),
    Map("vT1_0" -> "T1", "vT1_1" -> "T1", "vT0_0" -> "T0", "vT0_1" -> "T0"),
    List(
      (
        "vT1_0",
        CtorCall(
          Spanned("T1", Span(-1, -1)),
          List(
            (
              Spanned("f0", Span(-1, -1)),
              CtorCall(Spanned("NoneT1", Span(-1, -1)), List(), Span(-1, -1))
            ),
            (
              Spanned("f1", Span(-1, -1)),
              CtorCall(Spanned("NoneT0", Span(-1, -1)), List(), Span(-1, -1))
            ),
            (
              Spanned("f2", Span(-1, -1)),
              CtorCall(Spanned("NoneT1", Span(-1, -1)), List(), Span(-1, -1))
            ),
            (
              Spanned("f3", Span(-1, -1)),
              CtorCall(Spanned("NoneT1", Span(-1, -1)), List(), Span(-1, -1))
            )
          ),
          Span(-1, -1)
        )
      ),
      (
        "vT1_1",
        CtorCall(
          Spanned("T1", Span(-1, -1)),
          List(
            (
              Spanned("f0", Span(-1, -1)),
              CtorCall(Spanned("NoneT1", Span(-1, -1)), List(), Span(-1, -1))
            ),
            (
              Spanned("f1", Span(-1, -1)),
              CtorCall(Spanned("NoneT0", Span(-1, -1)), List(), Span(-1, -1))
            ),
            (
              Spanned("f2", Span(-1, -1)),
              CtorCall(Spanned("NoneT1", Span(-1, -1)), List(), Span(-1, -1))
            ),
            (
              Spanned("f3", Span(-1, -1)),
              CtorCall(Spanned("NoneT1", Span(-1, -1)), List(), Span(-1, -1))
            )
          ),
          Span(-1, -1)
        )
      ),
      (
        "vT0_0",
        CtorCall(
          Spanned("T0", Span(-1, -1)),
          List(
            (
              Spanned("f0", Span(-1, -1)),
              CtorCall(Spanned("NoneT0", Span(-1, -1)), List(), Span(-1, -1))
            ),
            (
              Spanned("f1", Span(-1, -1)),
              CtorCall(Spanned("NoneT1", Span(-1, -1)), List(), Span(-1, -1))
            ),
            (
              Spanned("f2", Span(-1, -1)),
              CtorCall(Spanned("NoneT0", Span(-1, -1)), List(), Span(-1, -1))
            ),
            (
              Spanned("f3", Span(-1, -1)),
              CtorCall(Spanned("NoneT1", Span(-1, -1)), List(), Span(-1, -1))
            )
          ),
          Span(-1, -1)
        )
      ),
      (
        "vT0_1",
        CtorCall(
          Spanned("T0", Span(-1, -1)),
          List(
            (
              Spanned("f0", Span(-1, -1)),
              CtorCall(Spanned("NoneT0", Span(-1, -1)), List(), Span(-1, -1))
            ),
            (
              Spanned("f1", Span(-1, -1)),
              CtorCall(Spanned("NoneT1", Span(-1, -1)), List(), Span(-1, -1))
            ),
            (
              Spanned("f2", Span(-1, -1)),
              CtorCall(Spanned("NoneT0", Span(-1, -1)), List(), Span(-1, -1))
            ),
            (
              Spanned("f3", Span(-1, -1)),
              CtorCall(Spanned("NoneT1", Span(-1, -1)), List(), Span(-1, -1))
            )
          ),
          Span(-1, -1)
        )
      )
    ),
    List(
      GenStmt.Assign("vT0_0", "f1", "T1", "vT1_0"),
      GenStmt.Assign("vT0_1", "f1", "T1", "vT1_0")
    )
  )

  def fuzz(prog: GeneratedProgram, out: String): Unit = {
    val ast = prog.asAst
    val settings = Settings(includeMemcheck = true)
    try { ExecTests.valgrindCheck(ast, None, None, settings = settings) }
    catch {
      case e =>
        given typer: Typer = Typer.resolveAllTypes(ast)
        val c = Translator.toC(ast, settings)
        Files.write(Path.of("fuzz-out", out), c.getBytes())
        throw e
    }
  }

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
      GeneratedProgram(
        allTypes,
        allVars.flatMap((typ, vars) => vars.keySet.map(_ -> typ)).toMap,
        sccs.flatMap(
          _.filterNot(_.name.startsWith("Opt"))
            .flatMap(typ => allVars(typ.name))
        ),
        assignExprs
      )
    }

    forAll(genFull) { generated => fuzz(generated, "no-intermediate.c") }
  }

  property("With intermediate checks", Slow) {
    given PropertyCheckConfiguration =
      PropertyCheckConfiguration(minSize = 10, sizeRange = 60)
    given Shrink[GeneratedProgram] = GenUtil.shrinkGenerated

    val checkRate = 0.1

    // TODO maybe randomness isn't needed here, just insert one check every few statements or something
    def insertValgrindChecks(stmts: List[GenStmt]): Gen[List[GenStmt]] = {
      GenUtil.sequence(stmts.map { stmt =>
        Gen.prob(checkRate).map { insert =>
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
      assignExprs = Random(seed).shuffle(assignExprsOrdered)
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

    forAll(genFull) { generated => fuzz(generated, "with-intermediate.c") }
  }
}
