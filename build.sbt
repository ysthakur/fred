import snapshot4s.BuildInfo.snapshot4sVersion

val scala3Version = "3.5.1"

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = project
  .in(file("."))
  .enablePlugins(Snapshot4sPlugin)
  .settings(
    name := "fred",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-parse" % "0.3.9",
      "org.scalactic" %% "scalactic" % "3.2.19",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.scalatestplus" %% "scalacheck-1-18" % "3.2.19.0" % Test,
      "com.siriusxm" %% "snapshot4s-scalatest" % snapshot4sVersion % Test,
      "com.lihaoyi" %% "pprint" % "0.9.0" % Test,
    )
  )
