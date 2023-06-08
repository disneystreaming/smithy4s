val scala213 = "2.13.11"
val scala3 = "3.3.0"

val expectedWildcardArgument =
  settingKey[String]("The expected value of smithy4sWildcardArgument")

val checkProject = project
  .in(file("check"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    (Compile / expectedWildcardArgument) := "",
    TaskKey[Unit]("check") := {
      val got = (Compile / smithy4sWildcardArgument).value
      val expected = (Compile / expectedWildcardArgument).value
      val s = streams.value
      s.log.info(
        s"testing (version: ${(Compile / scalaVersion).value}) (scalacOptions: ${(Compile / scalacOptions).value})"
      )
      if (expected != got) {
        sys.error(s"""expected wildcard argument "$expected", got "$got"""")
      }
    }
  )

val root = project
  .in(file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    crossScalaVersions := List(scala213, scala3),
    scalacOptions ++= {
      if (scalaVersion.value == scala3) {
        Seq("-Xfatal-warnings", "-source:future")
      } else {
        Seq("-Xfatal-warnings")
      }
    },
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
    )
  )

// usage: [expected wildcard] [scala version] [...scalac options]
ThisBuild / commands += Command.args("check", "check") {
  case (state, expected +: scalaVersion +: scalacOptions) =>
    val options = scalacOptions.map(o => '"' + o + '"').mkString(", ")
    val commands = List(
      s"""set checkProject / Compile / scalaVersion := "$scalaVersion"""",
      s"""set checkProject / Compile / expectedWildcardArgument := "$expected"""",
      s"""set checkProject / Compile / scalacOptions := Seq($options)""",
      s"""checkProject / check"""
    )
    commands ::: state
}
