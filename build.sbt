import org.scalajs.jsenv.nodejs.NodeJSEnv

import java.io.File
import sys.process._

ThisBuild / commands ++= createBuildCommands(allModules)
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"
ThisBuild / dynverSeparator := "-"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / mimaBaseVersion := "0.15"
ThisBuild / testFrameworks += new TestFramework("weaver.framework.CatsEffect")
import Smithy4sPlugin._

val latest2ScalaVersions = List(Scala213, Scala3)
val allJvmScalaVersions = List(Scala212, Scala213, Scala3)
val allJsScalaVersions = latest2ScalaVersions
val allNativeScalaVersions = List(Scala3)
val jvmScala2Versions = List(Scala212, Scala213)
val buildtimejvmScala2Versions = List(Scala212, Scala213)

Global / organizationName := "Disney Streaming"
Global / startYear := Some(2021)
Global / licenses := Seq(
  "TOST-1.0" -> new URL("https://disneystreaming.github.io/TOST-1.0.txt")
)

sonatypeCredentialHost := "s01.oss.sonatype.org"

ThisBuild / version := {
  if (!sys.env.contains("CI")) "dev"
  else (ThisBuild / version).value
}

lazy val root = project
  .in(file("."))
  .aggregate(allModules: _*)
  // .disablePlugins(Smithy4sPlugin)
  .enablePlugins(ScalafixPlugin)
  .settings(Smithy4sPlugin.doNotPublishArtifact)
  .settings(
    pushRemoteCache := {},
    pullRemoteCache := {},
    Compile / packageCache / moduleName := "smithy4s-root"
  )

lazy val allModules = Seq(
  core.projectRefs,
  codegen.projectRefs,
  json.projectRefs,
  example.projectRefs,
  tests.projectRefs,
  http4s.projectRefs,
  `http4s-swagger`.projectRefs,
  decline.projectRefs,
  codegenPlugin.projectRefs,
  benchmark.projectRefs,
  protocol.projectRefs,
  protocolTests.projectRefs,
  openapi.projectRefs,
  `aws-kernel`.projectRefs,
  aws.projectRefs,
  `aws-http4s`.projectRefs,
  `codegen-cli`.projectRefs,
  dynamic.projectRefs,
  testUtils.projectRefs
).flatten

lazy val docs =
  projectMatrix
    .in(file("modules/docs"))
    .enablePlugins(MdocPlugin, DocusaurusPlugin)
    .jvmPlatform(List(Scala213))
    .dependsOn(
      `codegen-cli`,
      http4s,
      `http4s-swagger`,
      `aws-http4s` % "compile -> compile,test"
    )
    .settings(
      mdocIn := (ThisBuild / baseDirectory).value / "modules" / "docs" / "src",
      mdocVariables := Map(
        "VERSION" -> (if (isSnapshot.value)
                        previousStableVersion.value.getOrElse(
                          throw new Exception(
                            "No previous version found from dynver"
                          )
                        )
                      else version.value),
        "SCALA_VERSION" -> scalaVersion.value,
        "HTTP4S_VERSION" -> Dependencies.Http4s.http4sVersion.value
      ),
      isCE3 := true,
      libraryDependencies ++= Seq(
        Dependencies.Http4s.emberClient.value,
        Dependencies.Http4s.emberServer.value
      ),
      Compile / sourceGenerators := Seq(genSmithyScala(Compile).taskValue),
      Compile / smithySpecs := Seq(
        (ThisBuild / baseDirectory).value / "sampleSpecs" / "hello.smithy"
      )
    )
    .settings(Smithy4sPlugin.doNotPublishArtifact)

val munitDeps = Def.setting {
  if (virtualAxes.value.contains(VirtualAxis.native)) {
    Seq(
      Dependencies.MunitMilestone.core.value % Test,
      Dependencies.MunitMilestone.scalacheck.value % Test
    )
  } else {
    Seq(
      Dependencies.Munit.core.value % Test,
      Dependencies.Munit.scalacheck.value % Test
    )
  }
}

/**
 * Protocol-agnostic, dependency-free core module, containing
 * only interfaces and other polymorphic constructs, allowing for the
 * traversal of models and services.
 *
 * These interfaces are implemented by the generated code, and leveraged
 * by protocol-specific interpreters.
 *
 * This module also contains a Scala representation of smithy's Standard Library
 * (under the smithy.api namespace), which cointains a number of types and traits
 * (hints in smithy4s) that are commonly used (such as http-specific traits, etc)
 *
 * In most cases, it this module the only dependency required to compile the generated
 * code.
 */
lazy val core = projectMatrix
  .in(file("modules/core"))
  .settings(
    Test / fork := virtualAxes.value.contains(VirtualAxis.jvm),
    isMimaEnabled := true,
    allowedNamespaces := Seq(
      "smithy.api",
      "smithy.waiters",
      "smithy4s.api"
    ),
    genDiscoverModels := true,
    Compile / sourceGenerators := Seq(genSmithyScala(Compile).taskValue),
    Compile / sourceGenerators += sourceDirectory
      .map(Boilerplate.gen(_, Boilerplate.BoilerplateModule.Core))
      .taskValue,
    libraryDependencies ++= Seq(
      Dependencies.collectionsCompat.value,
      Dependencies.Cats.core.value % Test
    ),
    libraryDependencies ++= munitDeps.value,
    Test / allowedNamespaces := Seq(
      "smithy4s.example",
      "smithy4s.example.collision",
    ),
    Test / smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "metadata.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "recursive.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "bodies.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "misc.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "product.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "weather.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "discriminated.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "untagged.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "packedInputs.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "errors.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "example.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "adtMember.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "namecollision.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "reservednames.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "enums.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "defaults.smithy"
    ),
    (Test / sourceGenerators) := Seq(genSmithyScala(Test).taskValue),
    Compile / packageSrc / mappings ++= {
      val base = (Compile / sourceManaged).value
      val files = (Compile / managedSources).value
      files
        .map(f => (f, f.relativeTo(base)))
        // this excludes modules/core/src/generated/PartiallyAppliedStruct.scala
        .collect { case (f, Some(relF)) => f -> relF.getPath() }
    }
  )
  .jvmPlatform(allJvmScalaVersions, jvmDimSettings)
  .jsPlatform(allJsScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

/**
 * Smithy4s specific scalacheck integration.
 */
lazy val scalacheck = projectMatrix
  .in(file("modules/scalacheck"))
  .dependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.collectionsCompat.value,
      Dependencies.Scalacheck.scalacheck.value
    ),
    libraryDependencies ++= munitDeps.value,
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
  )
  .jvmPlatform(allJvmScalaVersions, jvmDimSettings)
  .jsPlatform(allJsScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

/**
 * The aws-specific core a library. Contains the generated code for AWS specific
 * traits, the instances of which contain metadata required to run the AWS signing
 * algorithm.
 *
 * Also contains basic data types and functionality.
 */
lazy val `aws-kernel` = projectMatrix
  .in(file("modules/aws-kernel"))
  .dependsOn(core)
  .settings(
    isCE3 := false,
    libraryDependencies ++= Seq(
      Dependencies.Weaver.cats.value % Test,
      Dependencies.Weaver.scalacheck.value % Test
    ),
    genDiscoverModels := true,
    Compile / allowedNamespaces := Seq(
      "aws.api",
      "aws.auth",
      "aws.customizations",
      "aws.protocols"
    ),
    Compile / sourceGenerators := Seq(genSmithyScala(Compile).taskValue),
    Test / fork := true,
    Test / envVars ++= Map("TEST_VAR" -> "hello")
  )
  .jvmPlatform(latest2ScalaVersions, jvmDimSettings)
  .jsPlatform(
    latest2ScalaVersions,
    jsDimSettings ++ Seq(
      Test / jsEnv := new NodeJSEnv(
        NodeJSEnv.Config().withEnv(Map("TEST_VAR" -> "hello"))
      )
    )
  )

/**
 * cats-effect specific abstractions of AWS protocol interpreters and constructs
 */
lazy val aws = projectMatrix
  .in(file("modules/aws"))
  .dependsOn(`aws-kernel`, json)
  .settings(
    isCE3 := true,
    libraryDependencies ++= {
      // Only building this module against CE3
      Seq(
        Dependencies.Fs2.core.value,
        Dependencies.Weaver.cats.value % Test,
        Dependencies.Weaver.scalacheck.value % Test
      )
    },
    Test / fork := true
  )
  .jvmPlatform(latest2ScalaVersions, jvmDimSettings)
  .jsPlatform(latest2ScalaVersions, jsDimSettings)

/**
 * http4s-specific implementation of aws protocols. This module exposes generic methods
 * to acquire instances of AWS clients.
 *
 * This module does not contain the service-specific instances (ie, it does not contain
 * DynamoDB or Kinesis specific constructs). It works against the types generated in the
 * `aws` module to provide interpreters that can "run" AWS requests.
 */
lazy val `aws-http4s` = projectMatrix
  .in(file("modules/aws-http4s"))
  .dependsOn(aws)
  .settings(
    isCE3 := true,
    Test / genDiscoverModels := true,
    libraryDependencies ++= {
      Seq(
        Dependencies.Http4s.client.value,
        Dependencies.Http4s.emberClient.value % Test
      )
    },
    Test / allowedNamespaces := Seq(),
    Test / sourceGenerators := Seq(genSmithyScala(Test).taskValue),
    Test / smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "dynamodb.2012-08-10.json",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "lambda.json"
    )
  )
  .jvmPlatform(latest2ScalaVersions, jvmDimSettings)
  .jsPlatform(latest2ScalaVersions, jsDimSettings)

/**
 * This module contains the logic used at build time for reading smithy
 * models and rendering Scala (or openapi) code.
 */
lazy val codegen = projectMatrix
  .in(file("modules/codegen"))
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(openapi)
  .jvmPlatform(buildtimejvmScala2Versions, jvmDimSettings)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      scalaBinaryVersion,
      "smithyVersion" -> Dependencies.Smithy.smithyVersion
    ),
    buildInfoPackage := "smithy4s.codegen",
    libraryDependencies ++= Seq(
      Dependencies.Cats.core.value,
      Dependencies.Smithy.model,
      Dependencies.Smithy.build,
      Dependencies.Smithy.awsTraits,
      Dependencies.Smithy.waiters,
      "com.lihaoyi" %% "os-lib" % "0.8.1",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.2.0",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "io.get-coursier" %% "coursier" % "2.0.16"
    ),
    libraryDependencies ++= munitDeps.value,
    scalacOptions := scalacOptions.value
      .filterNot(Seq("-Ywarn-value-discard", "-Wvalue-discard").contains)
  )

/**
 * This module is the command-line-interface to the codegen module, that
 * can be used independently of build tools (or that build tools can choose
 * to delegate to in order to implement plugins)
 */
lazy val `codegen-cli` = projectMatrix
  .in(file("modules/codegen-cli"))
  .dependsOn(codegen)
  .jvmPlatform(List(Scala213), jvmDimSettings)
  .settings(
    isCE3 := true,
    libraryDependencies ++= Seq(
      Dependencies.Decline.core.value,
      Dependencies.Weaver.cats.value % Test
    )
  )

/**
 * SBT plugin wrapping calls to the functions provided by the codegen module.
 */
lazy val codegenPlugin = (projectMatrix in file("modules/codegen-plugin"))
  .enablePlugins(SbtPlugin)
  .dependsOn(codegen)
  .jvmPlatform(
    scalaVersions = List(Scala212),
    jvmDimSettings
  )
  .settings(
    name := "sbt-codegen",
    sbtPlugin := true,
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    Compile / unmanagedSources / excludeFilter := { f =>
      Glob("**/sbt-test/**").matches(f.toPath)
    },
    publishLocal := {
      // make sure that core and codegen are published before the
      // plugin is published
      // this allows running `scripted` alone
      val _ = List(
        // for the code being built
        (core.jvm(Scala213) / publishLocal).value,
        (dynamic.jvm(Scala213) / publishLocal).value,
        (codegen.jvm(Scala213) / publishLocal).value,
        // dependency of codegen
        (openapi.jvm(Scala213) / publishLocal).value,

        // for sbt
        (codegen.jvm(Scala212) / publishLocal).value,
        (openapi.jvm(Scala212) / publishLocal).value,
        (protocol.jvm(autoScalaLibrary = false) / publishLocal).value
      )
      publishLocal.value
    },
    scriptedBufferLog := false
  )

lazy val decline = (projectMatrix in file("modules/decline"))
  .settings(
    name := "decline",
    isCE3 := true,
    libraryDependencies ++= List(
      Dependencies.Cats.core.value,
      Dependencies.Decline.effect.value,
      Dependencies.Weaver.cats.value % Test
    )
  )
  .dependsOn(json)
  .jvmPlatform(allJvmScalaVersions, jvmDimSettings)
  .jsPlatform(allJsScalaVersions, jsDimSettings)

/**
 * This module contains the smithy specification of a bunch of types
 * that are not provided by the smithy standard library, but are useful
 * nonetheless and pretty common (such as UUID).
 *
 * It also contains the definition of a custom protocol implemented in this
 * library, which is a pretty basic REST-JSON protocol. smithy4s provides
 * server and client side bindings for this protocol.
 */
lazy val protocol = projectMatrix
  .in(file("modules/protocol"))
  .jvmPlatform(
    autoScalaLibrary = false,
    scalaVersions = Seq.empty,
    settings = jvmDimSettings
  )
  .settings(
    Compile / packageSrc / mappings := (Compile / packageSrc / mappings).value
      .filterNot { case (file, path) =>
        path.equalsIgnoreCase("META-INF/smithy/manifest")
      },
    resolvers += Resolver.mavenLocal,
    libraryDependencies += Dependencies.Smithy.model,
    javacOptions ++= Seq("--release", "8")
  )

lazy val protocolTests = projectMatrix
  .in(file("modules/protocol-tests"))
  .jvmPlatform(Seq(Scala213), jvmDimSettings)
  .dependsOn(protocol)
  .settings(
    isCE3 := true,
    libraryDependencies ++= Seq(
      Dependencies.Weaver.cats.value % Test,
      Dependencies.Weaver.scalacheck.value % Test
    ),
    Test / fork := true
  )
  .settings(Smithy4sPlugin.doNotPublishArtifact)

/**
 * This modules contains utilities to dynamically instantiate
 * the interfaces provide by smithy4s, based on data from dynamic
 * Model instances.
 */
lazy val dynamic = projectMatrix
  .in(file("modules/dynamic"))
  .dependsOn(core % "test->test;compile->compile", testUtils % "test->compile")
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.8.1",
      Dependencies.Cats.core.value
    ),
    libraryDependencies ++= munitDeps.value,
    Compile / allowedNamespaces := Seq("smithy4s.dynamic.model"),
    Compile / smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "modules" / "dynamic" / "smithy" / "dynamic.smithy"
    ),
    Compile / sourceGenerators := Seq(genSmithyScala(Compile).taskValue),
    Test / allowedNamespaces := Seq("smithy4s.example"),
    Test / smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "kvstore.smithy"
    ),
    (Test / sourceGenerators) := Seq(genSmithyScala(Test).taskValue)
  )
  .jvmPlatform(
    allJvmScalaVersions,
    jvmDimSettings ++ Seq(
      Test / fork := true,
      // Forwarding cwd to tests
      Test / javaOptions += s"-Duser.dir=${sys.props("user.dir")}",
      libraryDependencies += Dependencies.Smithy.model
    )
  )
  .jsPlatform(allJsScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

/**
 * Module that contains the logic for generating "openapi views" of the
 * services that abide by some custom protocols provided by this library.
 */
lazy val openapi = projectMatrix
  .in(file("modules/openapi"))
  .jvmPlatform(buildtimejvmScala2Versions, jvmDimSettings)
  .dependsOn(protocol)
  .settings(
    isCE3 := true,
    libraryDependencies ++= Seq(
      Dependencies.Cats.core.value,
      Dependencies.Smithy.openapi,
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.8.1",
      Dependencies.Weaver.cats.value % Test
    )
  )

/**
 * Module that contains jsoniter-based encoders/decoders for the generated
 * types.
 */
lazy val json = projectMatrix
  .in(file("modules/json"))
  .dependsOn(
    core % "test->test;compile->compile",
    scalacheck % "test -> compile"
  )
  .settings(
    isMimaEnabled := true,
    libraryDependencies ++= Seq(
      Dependencies.Jsoniter.value
    ),
    libraryDependencies ++= munitDeps.value,
    Test / fork := virtualAxes.value.contains(VirtualAxis.jvm)
  )
  .jvmPlatform(allJvmScalaVersions, jvmDimSettings)
  .jsPlatform(allJsScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

/**
 * Module that contains http4s-specific client/server bindings for the
 * custom protocols provided by smithy4s.
 */
lazy val http4s = projectMatrix
  .in(file("modules/http4s"))
  .dependsOn(
    core,
    json,
    dynamic % "test->compile",
    tests % "test->compile",
    testUtils % "test->compile"
  )
  .settings(
    isCE3 := virtualAxes.value.contains(CatsEffect3Axis),
    isMimaEnabled := true,
    libraryDependencies ++= {
      val ce3 =
        if (isCE3.value) Seq(Dependencies.CatsEffect3.value)
        else Seq.empty

      ce3 ++ Seq(
        Dependencies.Http4s.core.value,
        Dependencies.Http4s.dsl.value,
        Dependencies.Http4s.client.value,
        Dependencies.Http4s.circe.value % Test,
        Dependencies.Weaver.cats.value % Test
      )
    },
    moduleName := {
      if (virtualAxes.value.contains(CatsEffect2Axis))
        moduleName.value + "-ce2"
      else moduleName.value
    }
  )
  .http4sPlatform(allJvmScalaVersions, jvmDimSettings)

/**
 * Module that contains a function to derive a documentation endpoint
 */
lazy val `http4s-swagger` = projectMatrix
  .in(file("modules/http4s-swagger"))
  .dependsOn(http4s)
  .settings(
    isCE3 := virtualAxes.value.contains(CatsEffect3Axis),
    libraryDependencies ++= {
      Seq(
        Dependencies.Weaver.cats.value % Test,
        Dependencies.Webjars.swaggerUi,
        Dependencies.Webjars.webjarsLocator
      )
    },
    moduleName := {
      if (virtualAxes.value.contains(CatsEffect2Axis))
        moduleName.value + "-ce2"
      else moduleName.value
    }
  )
  .http4sJvmPlatform(allJvmScalaVersions, jvmDimSettings)

lazy val testUtils = projectMatrix
  .in(file("modules/test-utils"))
  .dependsOn(core)
  .settings(Smithy4sPlugin.doNotPublishArtifact)
  .settings(
    libraryDependencies += Dependencies.Cats.core.value
  )
  .jvmPlatform(allJvmScalaVersions, jvmDimSettings)
  .jsPlatform(allJsScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

/**
 * Generic tests aimed at testing the implementations of the custom protocols
 * provided by smithy4s.
 */
lazy val tests = projectMatrix
  .in(file("modules/tests"))
  .dependsOn(core)
  .settings(
    isCE3 := virtualAxes.value.contains(CatsEffect3Axis),
    libraryDependencies ++= {
      val ce3 =
        if (isCE3.value) Seq(Dependencies.CatsEffect3.value)
        else Seq.empty

      ce3 ++ Seq(
        Dependencies.Http4s.core.value,
        Dependencies.Http4s.dsl.value,
        Dependencies.Http4s.emberClient.value,
        Dependencies.Http4s.emberServer.value,
        Dependencies.Http4s.circe.value,
        Dependencies.Weaver.cats.value
      )
    },
    Compile / smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "pizza.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "weather.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "recursiveInput.smithy"
    ),
    moduleName := {
      if (virtualAxes.value.contains(CatsEffect2Axis))
        moduleName.value + "-ce2"
      else moduleName.value
    },
    (Compile / sourceGenerators) := Seq(genSmithyScala(Compile).taskValue)
  )
  .http4sPlatform(allJvmScalaVersions, jvmDimSettings)

/**
 * Example application using the custom REST-JSON protocol provided by
 * smithy4s.
 */
lazy val example = projectMatrix
  .in(file("modules/example"))
  .dependsOn(`http4s-swagger`)
  .disablePlugins(ScalafixPlugin)
  .disablePlugins(HeaderPlugin)
  .settings(
    Compile / allowedNamespaces := Seq(
      "smithy4s.example",
      "smithy4s.example.import_test",
      "smithy4s.example.imp",
      "smithy4s.example.error",
      "smithy4s.example.common",
      "smithy4s.example.collision"
    ),
    smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "example.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "errors.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "streaming.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "operation.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "import.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "importerror.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "adtMember.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "brands.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "brandscommon.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "refined.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "enums.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "reservednames.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "namecollision.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "mixins.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "defaults.smithy"
    ),
    Compile / resourceDirectory := (ThisBuild / baseDirectory).value / "modules" / "example" / "resources",
    isCE3 := true,
    libraryDependencies += Dependencies.Http4s.emberServer.value,
    (Compile / sourceGenerators) := Seq(genSmithyScala(Compile).taskValue),
    (Compile / resourceGenerators) := Seq(
      genSmithyResources(Compile).taskValue
    ),
    genSmithyOutput := ((ThisBuild / baseDirectory).value / "modules" / "example" / "src"),
    genSmithyOpenapiOutput := (Compile / resourceDirectory).value
  )
  .jvmPlatform(List(Scala213), jvmDimSettings)
  .settings(Smithy4sPlugin.doNotPublishArtifact)

/**
 * Pretty primitive benchmarks to test that we're not doing anything drastically
 * slow.
 */
lazy val benchmark = projectMatrix
  .in(file("modules/benchmark"))
  .enablePlugins(JmhPlugin)
  .dependsOn(
    http4s % "compile -> compile,test",
    `scalacheck`
  )
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Circe.generic.value
    ),
    smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "benchmark.smithy"
    ),
    (Compile / sourceGenerators) := Seq(genSmithyScala(Compile).taskValue)
  )
  .jvmPlatform(List(Scala213), jvmDimSettings)
  .settings(Smithy4sPlugin.doNotPublishArtifact)

val isCE3 = settingKey[Boolean]("Is the current build using CE3?")

lazy val Dependencies = new {

  val collectionsCompat =
    Def.setting(
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.8.1"
    )

  val Jsoniter =
    Def.setting(
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-core" % "2.17.0"
    )

  val Smithy = new {
    val smithyVersion = "1.23.1"
    val model = "software.amazon.smithy" % "smithy-model" % smithyVersion
    val build = "software.amazon.smithy" % "smithy-build" % smithyVersion
    val awsTraits =
      "software.amazon.smithy" % "smithy-aws-traits" % smithyVersion
    val openapi = "software.amazon.smithy" % "smithy-openapi" % smithyVersion
    val waiters = "software.amazon.smithy" % "smithy-waiters" % smithyVersion
  }

  val Cats = new {
    val core: Def.Initialize[ModuleID] =
      Def.setting("org.typelevel" %%% "cats-core" % "2.8.0")
  }

  object Decline {
    val core = Def.setting("com.monovore" %%% "decline-effect" % "2.3.0")
    val effect = Def.setting("com.monovore" %%% "decline-effect" % "2.3.0")
  }
  object Fs2 {
    val core: Def.Initialize[ModuleID] =
      Def.setting("co.fs2" %%% "fs2-core" % "3.2.12")
  }

  val Circe = new {
    val generic: Def.Initialize[ModuleID] =
      Def.setting("io.circe" %%% "circe-generic" % "0.14.2")
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
    Def.setting("org.typelevel" %%% "cats-effect" % "3.3.14")

  object Http4s {
    val http4sVersion = Def.setting(if (isCE3.value) "0.23.15" else "0.22.14")

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

    val weaverVersion = Def.setting(if (isCE3.value) "0.7.15" else "0.6.15")

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
    val version = "1.16.0"
    val scalacheck =
      Def.setting("org.scalacheck" %%% "scalacheck" % version)
  }

  object Webjars {
    val swaggerUi: ModuleID = "org.webjars.npm" % "swagger-ui-dist" % "4.14.0"

    val webjarsLocator: ModuleID = "org.webjars" % "webjars-locator" % "0.42"
  }

}

lazy val smithySpecs = SettingKey[Seq[File]]("smithySpecs")
lazy val genSmithyOutput = SettingKey[File]("genSmithyOutput")
lazy val genSmithyOpenapiOutput = SettingKey[File]("genSmithyOpenapiOutput")
lazy val allowedNamespaces = SettingKey[Seq[String]]("allowedNamespaces")
lazy val genSmithyDependencies =
  SettingKey[Seq[String]]("genSmithyDependencies")
lazy val genDiscoverModels = SettingKey[Boolean]("genDiscoverModels")

(ThisBuild / smithySpecs) := Seq.empty

def genSmithyScala(config: Configuration) = genSmithyImpl(config).map(_._1)
def genSmithyResources(config: Configuration) = genSmithyImpl(config).map(_._2)

/**
 * Dogfooding task that calls the codegen module, to generate smithy standard
 * library code, aws-specific code.
 */
def genSmithyImpl(config: Configuration) = Def.task {
  val inputFiles = (config / smithySpecs).value
  val outputDir = (config / genSmithyOutput).?.value
    .getOrElse((config / sourceManaged).value)
    .getAbsolutePath()
  val resourceOutputDir =
    (config / genSmithyOpenapiOutput).?.value
      .getOrElse((config / resourceManaged).value)
      .getAbsolutePath()
  val allowedNS = (config / allowedNamespaces).?.value.filterNot(_.isEmpty)
  val smithyDeps =
    (config / genSmithyDependencies).?.value.getOrElse(List.empty)
  val discoverModels =
    (config / genDiscoverModels).?.value.getOrElse(false)

  val codegenCp =
    (`codegen-cli`.jvm(Smithy4sPlugin.Scala213) / Compile / fullClasspath).value
      .map(_.data)

  val mc = "smithy4s.codegen.cli.Main"
  val s = streams.value

  def untupled[A, B, C](f: ((A, B)) => C): (A, B) => C = (a, b) => f((a, b))

  val cached =
    Tracked.inputChanged[FilesInfo[HashFileInfo], Seq[File]](
      s.cacheStoreFactory.make("input")
    ) {
      untupled {
        Tracked
          .lastOutput[(Boolean, FilesInfo[HashFileInfo]), Seq[File]](
            s.cacheStoreFactory.make("output")
          ) { case ((changed, files), outputs) =>
            if (changed || outputs.isEmpty) {
              val inputs = inputFiles.map(_.getAbsolutePath()).toList
              val args =
                List("--output", outputDir) ++
                  List("--resource-output", resourceOutputDir) ++
                  (if (discoverModels) List("--discover-models") else Nil) ++
                  (if (allowedNS.isDefined)
                     List("--allowed-ns", allowedNS.get.mkString(","))
                   else Nil) ++ inputs

              val cp = codegenCp
                .map(_.getAbsolutePath())
                .mkString(":")

              val res =
                ("java" :: "-cp" :: cp :: mc :: "generate" :: args).lineStream.toList
              res.map(new File(_))
            } else outputs.getOrElse(Seq.empty)
          }
      }
    }

  val trackedFiles = inputFiles ++ codegenCp.allPaths.get()
  cached(FilesInfo(trackedFiles.map(FileInfo.hash(_)).toSet))
    .partition(_.ext == "scala")
}

addCommandAlias(
  "ci",
  "versionDump; clean; scalafmtCheckAll; headerCheck; test; publishLocal; scripted"
)

addCommandAlias(
  "preCI",
  "scalafmtAll; scalafmtSbt; scalafix --rules OrganizeImports"
)

lazy val versionDump =
  taskKey[Unit]("Dumps the version in a file named version")

versionDump := {
  val file = (ThisBuild / baseDirectory).value / "version"
  IO.write(file, (Compile / version).value)
}

ThisBuild / commands += Command.command("release") { state =>
  "publishSigned" ::
    "sonatypeBundleRelease" :: state
}
