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
      "org.scalameta" %% "munit" % "1.0.0" % Test,
      "com.siriusxm" %% "snapshot4s-munit" % snapshot4sVersion % Test
    )
  )
