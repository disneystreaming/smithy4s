import sbt._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import Smithy4sBuildPlugin.autoImport.isCE3

object Dependencies {

  val collectionsCompat =
    Def.setting(
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.9.0"
    )

  val Jsoniter =
    Def.setting(
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-core" % "2.20.3"
    )

  val Smithy = new {
    val org = "software.amazon.smithy"
    val smithyVersion = "1.26.4"
    val model = org % "smithy-model" % smithyVersion
    val testTraits = org % "smithy-protocol-test-traits" % smithyVersion
    val build = org % "smithy-build" % smithyVersion
    val awsTraits = org % "smithy-aws-traits" % smithyVersion
    val waiters = org % "smithy-waiters" % smithyVersion
  }

  val Alloy = new {
    val org = "com.disneystreaming.alloy"
    val alloyVersion = "0.1.8"
    val core = org % "alloy-core" % alloyVersion
    val openapi = org %% "alloy-openapi" % alloyVersion
  }

  val Cats = new {
    val core: Def.Initialize[ModuleID] =
      Def.setting("org.typelevel" %%% "cats-core" % "2.9.0")
  }

  object Circe {
    val circeVersion = "0.14.3"
    val parser = Def.setting("io.circe" %%% "circe-parser" % circeVersion)
    val generic = Def.setting("io.circe" %%% "circe-generic" % circeVersion)
  }

  object Decline {
    val declineVersion = "2.4.1"

    val core = Def.setting("com.monovore" %%% "decline" % declineVersion)
    val effect =
      Def.setting("com.monovore" %%% "decline-effect" % declineVersion)
  }
  object Fs2 {
    val core: Def.Initialize[ModuleID] =
      Def.setting("co.fs2" %%% "fs2-core" % "3.4.0")
  }

  object Mill {
    val millVersion = "0.10.10"

    val scalalib = "com.lihaoyi" %% "mill-scalalib" % millVersion
    val main = "com.lihaoyi" %% "mill-main" % millVersion
    val mainApi = "com.lihaoyi" %% "mill-main-api" % millVersion
    val mainTestkit = "com.lihaoyi" %% "mill-main-testkit" % millVersion % Test
  }

  object Pprint {
    val pprintVersion = "0.8.1"
    val core = Def.setting("com.lihaoyi" %%% "pprint" % pprintVersion)
  }

  /*
   * we override the version to use the fix included in
   * https://github.com/typelevel/cats-effect/pull/2945
   * it allows us to use UUIDGen instead of calling
   * UUID.randomUUID manually
   *
   * we also provide a 2.12 shim under:
   * modules/tests/src-ce2/UUIDGen.scala
   */
  val CatsEffect3: Def.Initialize[ModuleID] =
    Def.setting("org.typelevel" %%% "cats-effect" % "3.4.4")

  object Http4s {
    val http4sVersion = Def.setting(if (isCE3.value) "0.23.17" else "0.22.15")

    val emberServer: Def.Initialize[ModuleID] =
      Def.setting("org.http4s" %%% "http4s-ember-server" % http4sVersion.value)
    val emberClient: Def.Initialize[ModuleID] =
      Def.setting("org.http4s" %%% "http4s-ember-client" % http4sVersion.value)
    val circe: Def.Initialize[ModuleID] =
      Def.setting("org.http4s" %%% "http4s-circe" % http4sVersion.value)
    val core: Def.Initialize[ModuleID] =
      Def.setting("org.http4s" %%% "http4s-core" % http4sVersion.value)
    val dsl: Def.Initialize[ModuleID] =
      Def.setting("org.http4s" %%% "http4s-dsl" % http4sVersion.value)
    val client: Def.Initialize[ModuleID] =
      Def.setting("org.http4s" %%% "http4s-client" % http4sVersion.value)
  }

  object Weaver {

    val weaverVersion = Def.setting(if (isCE3.value) "0.8.1" else "0.6.15")

    val cats: Def.Initialize[ModuleID] =
      Def.setting("com.disneystreaming" %%% "weaver-cats" % weaverVersion.value)

    val scalacheck: Def.Initialize[ModuleID] =
      Def.setting(
        "com.disneystreaming" %%% "weaver-scalacheck" % weaverVersion.value
      )
  }

  class MunitCross(munitVersion: String) {
    val core: Def.Initialize[ModuleID] =
      Def.setting("org.scalameta" %%% "munit" % munitVersion)
    val scalacheck: Def.Initialize[ModuleID] =
      Def.setting("org.scalameta" %%% "munit-scalacheck" % munitVersion)
  }
  object Munit extends MunitCross("0.7.29")
  object MunitMilestone extends MunitCross("1.0.0-M6")

  val Scalacheck = new {
    val scalacheckVersion = "1.16.0"
    val scalacheck =
      Def.setting("org.scalacheck" %%% "scalacheck" % scalacheckVersion)
  }

  object Webjars {
    val swaggerUi: ModuleID = "org.webjars.npm" % "swagger-ui-dist" % "4.15.5"

    val webjarsLocator: ModuleID = "org.webjars" % "webjars-locator" % "0.42"
  }

}
