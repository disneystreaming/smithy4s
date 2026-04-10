import sbt._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {

  val collectionsCompat =
    Def.setting(
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.13.0"
    )

  val Jsoniter = new {
    val org = "com.github.plokhotnyuk.jsoniter-scala"
    val jsoniterScalaVersion = "2.37.10"
    val core = Def.setting(org %%% "jsoniter-scala-core" % jsoniterScalaVersion)
    val macros = Def.setting(
      org %%% "jsoniter-scala-macros" % jsoniterScalaVersion % "compile-internal"
    )
  }

  val Smithy = new {
    val org = "software.amazon.smithy"
    val smithyVersion = "1.66.0"
    val model = org % "smithy-model" % smithyVersion
    val testTraits = org % "smithy-protocol-test-traits" % smithyVersion
    val build = org % "smithy-build" % smithyVersion
    val diff = org % "smithy-diff" % smithyVersion
    val awsTraits = org % "smithy-aws-traits" % smithyVersion
    val waiters = org % "smithy-waiters" % smithyVersion
    val `aws-protocol-tests` = org % "smithy-aws-protocol-tests" % smithyVersion
  }

  val Alloy = new {
    val org = "com.disneystreaming.alloy"
    val alloyVersion = "0.3.37"
    val core = org % "alloy-core" % alloyVersion
    val openapi = org %% "alloy-openapi" % alloyVersion
    val protobuf = org % "alloy-protobuf" % alloyVersion
    val `protocol-tests` = org % "alloy-protocol-tests" % alloyVersion
  }

  val Smithytranslate = new {
    val org = "com.disneystreaming.smithy"
    val smithyTranslateVersion = "0.7.6"
    val proto = org %% "smithytranslate-proto" % smithyTranslateVersion
  }

  val Cats = new {
    val core: Def.Initialize[ModuleID] =
      Def.setting("org.typelevel" %%% "cats-core" % "2.13.0")
  }

  val Monocle = new {
    val core: Def.Initialize[ModuleID] =
      Def.setting("dev.optics" %% "monocle-core" % "3.3.0")
  }

  object Circe {
    val circeVersion = "0.14.14"
    val core = Def.setting("io.circe" %%% "circe-core" % circeVersion)
    val parser = Def.setting("io.circe" %%% "circe-parser" % circeVersion)
    val generic = Def.setting("io.circe" %%% "circe-generic" % circeVersion)
  }

  object Decline {
    val declineVersion = "2.5.0"

    val core = Def.setting("com.monovore" %%% "decline" % declineVersion)
    val effect =
      Def.setting("com.monovore" %%% "decline-effect" % declineVersion)
  }
  object Fs2 {
    val fs2Version = "3.13.0"

    val core: Def.Initialize[ModuleID] =
      Def.setting("co.fs2" %%% "fs2-core" % fs2Version)

    val io: Def.Initialize[ModuleID] =
      Def.setting("co.fs2" %%% "fs2-io" % fs2Version)
  }

  object Fs2Data {
    val xml: Def.Initialize[ModuleID] =
      Def.setting("org.gnieh" %%% "fs2-data-xml" % "1.13.0")
  }

  object Mill {
    def libs(v: String) = ("com.lihaoyi" %% "mill-libs" % v % Provided)
      .exclude("org.scala-lang", "scala-reflect")
      .exclude("org.scala-lang.modules", "scala-collection-compat_2.13")
      .exclude("org.scala-lang.modules", "scala-xml_2.13")
    def scalalib(v: String) = "com.lihaoyi" %% "mill-scalalib" % v % Provided
    def main(v: String) = "com.lihaoyi" %% "mill-main" % v % Provided
    def mainApi(v: String) = "com.lihaoyi" %% "mill-main-api" % v % Provided
    def mainTestkit(v: String) =
      if (v.startsWith("0.11")) {
        "com.lihaoyi" %% "mill-main-testkit" % v % Test
      } else {
        "com.lihaoyi" %% "mill-testkit" % v % Test
      }
  }

  object Pprint {
    val pprintVersion = "0.9.3"
    val core = Def.setting("com.lihaoyi" %%% "pprint" % pprintVersion)
  }

  val CatsEffect3: Def.Initialize[ModuleID] =
    Def.setting("org.typelevel" %%% "cats-effect" % "3.7.0")

  object Http4s {
    val http4sVersion = "0.23.34"

    val emberServer: Def.Initialize[ModuleID] =
      Def.setting("org.http4s" %%% "http4s-ember-server" % http4sVersion)
    val emberClient: Def.Initialize[ModuleID] =
      Def.setting("org.http4s" %%% "http4s-ember-client" % http4sVersion)
    val circe: Def.Initialize[ModuleID] =
      Def.setting("org.http4s" %%% "http4s-circe" % http4sVersion)
    val core: Def.Initialize[ModuleID] =
      Def.setting("org.http4s" %%% "http4s-core" % http4sVersion)
    val dsl: Def.Initialize[ModuleID] =
      Def.setting("org.http4s" %%% "http4s-dsl" % http4sVersion)
    val client: Def.Initialize[ModuleID] =
      Def.setting("org.http4s" %%% "http4s-client" % http4sVersion)
  }

  object Weaver {

    val weaverVersion = "0.12.0"

    val cats: Def.Initialize[ModuleID] =
      Def.setting("org.typelevel" %%% "weaver-cats" % weaverVersion)

    val scalacheck: Def.Initialize[ModuleID] =
      Def.setting(
        "org.typelevel" %%% "weaver-scalacheck" % weaverVersion
      )
  }

  class MunitCross(val munitVersion: String) {
    val core: Def.Initialize[ModuleID] =
      Def.setting("org.scalameta" %%% "munit" % munitVersion)
    val scalacheck: Def.Initialize[ModuleID] =
      Def.setting("org.scalameta" %%% "munit-scalacheck" % munitVersion)
  }
  object Munit extends MunitCross("1.1.0") {
    val diff: Def.Initialize[ModuleID] =
      Def.setting("org.scalameta" %%% "munit-diff" % munitVersion)
  }

  val Scalacheck = new {
    val scalacheckVersion = "1.18.1"
    val scalacheck =
      Def.setting("org.scalacheck" %%% "scalacheck" % scalacheckVersion)
  }

  val Slf4jSimple = "org.slf4j" % "slf4j-simple" % "2.0.17"

  object Webjars {
    val swaggerUi: ModuleID = "org.webjars.npm" % "swagger-ui-dist" % "5.20.3"

    val webjarsLocator: ModuleID = "org.webjars" % "webjars-locator" % "0.52"
  }

  object AwsSpecSummary {
    val org = "com.disneystreaming.smithy"
    val name = "aws-spec-summary"
    val awsSpecSummaryVersion = "2025.04.08"
    val value = org % name % awsSpecSummaryVersion
  }

  object Mima {
    val core = "com.typesafe" %% "mima-core" % "1.1.5"
  }

}
