import com.typesafe.tools.mima.core.ProblemFilters
import com.typesafe.tools.mima.core.MissingClassProblem
import com.typesafe.tools.mima.core.IncompatibleResultTypeProblem
import com.typesafe.tools.mima.core.IncompatibleMethTypeProblem
import _root_.java.util.stream.Collectors
import java.nio.file.Files
import sbt.internal.IvyConsole
import org.scalajs.jsenv.nodejs.NodeJSEnv

import java.io.File
import sys.process._

ThisBuild / commands ++= createBuildCommands(allModules)
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / dynverSeparator := "-"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / mimaBaseVersion := "0.18.0"

Global / onChangedBuildSource := ReloadOnSourceChanges

import Smithy4sBuildPlugin._

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
  if (!sys.env.contains("CI")) "dev-SNAPSHOT"
  else (ThisBuild / version).value
}

lazy val root = project
  .in(file("."))
  .aggregate(allModules: _*)
  // .disablePlugins(Smithy4sPlugin)
  .enablePlugins(ScalafixPlugin)
  .settings(Smithy4sBuildPlugin.doNotPublishArtifact)
  .settings(
    pushRemoteCache := {},
    pullRemoteCache := {},
    Compile / packageCache / moduleName := "smithy4s-root"
  )

lazy val allModules = Seq(
  core,
  codegen,
  millCodegenPlugin,
  json,
  xml,
  bootstrapped,
  tests,
  http4s,
  fs2,
  cats,
  `http4s-kernel`,
  `http4s-swagger`,
  decline,
  codegenPlugin,
  benchmark,
  protobuf,
  protocol,
  protocolTests,
  `aws-kernel`,
  `aws-http4s`,
  `codegen-cli`,
  dynamic,
  testUtils,
  guides,
  complianceTests
).flatMap(_.projectRefs)

lazy val docs =
  projectMatrix
    .in(file("modules/docs"))
    .enablePlugins(MdocPlugin, DocusaurusPlugin)
    .jvmPlatform(List(Scala213))
    .dependsOn(
      `codegen-cli`,
      http4s,
      `http4s-swagger`,
      decline,
      `aws-http4s` % "compile -> compile",
      complianceTests,
      dynamic,
      bootstrapped,
      protobuf
    )
    .settings(
      mdocIn := (ThisBuild / baseDirectory).value / "modules" / "docs" / "markdown",
      mdocVariables := Map(
        "VERSION" -> {
          sys.env
            .get("SMITHY4S_VERSION")
            .getOrElse {
              if (isSnapshot.value)
                previousStableVersion.value.getOrElse(
                  throw new Exception(
                    "No previous version found from dynver"
                  )
                )
              else version.value
            }
        },
        "SERVICE_PRODUCT_SPEC" -> IO
          .read(
            (ThisBuild / baseDirectory).value / "sampleSpecs" / "exampleServiceProduct.smithy"
          )
          .trim,
        "WEATHER_SERVICE_SPEC" -> IO
          .read(
            (ThisBuild / baseDirectory).value / "sampleSpecs" / "weather-docs.smithy"
          )
          .trim,
        "SCALA_VERSION" -> scalaVersion.value,
        "HTTP4S_VERSION" -> Dependencies.Http4s.http4sVersion,
        "GITHUB_BRANCH_URL" -> (for {
          serverUrl <- sys.env.get("GITHUB_SERVER_URL")
          repo <- sys.env.get("GITHUB_REPOSITORY")
          sha <- sys.env.get("GITHUB_SHA")
        } yield s"$serverUrl/$repo/blob/$sha/").getOrElse(
          "https://github.com/disneystreaming/smithy4s/tree/series/0.17/"
        ),
        "AWS_SPEC_VERSION" -> Dependencies.AwsSpecSummary.awsSpecSummaryVersion
      ),
      mdocExtraArguments := Seq("--check-link-hygiene"),
      libraryDependencies ++= Seq(
        Dependencies.Jsoniter.macros.value,
        Dependencies.Http4s.emberClient.value,
        Dependencies.Http4s.emberServer.value,
        Dependencies.Decline.effect.value,
        Dependencies.AwsSpecSummary.value,
        Dependencies.Monocle.core.value
      )
    )
    .settings(Smithy4sBuildPlugin.doNotPublishArtifact)

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
    isMimaEnabled := true,
    allowedNamespaces := Seq(
      "smithy.api",
      "smithy.waiters",
      "alloy",
      "alloy.common",
      "alloy.proto"
    ),
    smithy4sDependencies ++= Seq(
      Dependencies.Smithy.waiters
    ),
    genSmithy(Compile),
    Compile / sourceGenerators += {
      sourceDirectory
        .map(Boilerplate.gen(_, Boilerplate.BoilerplateModule.Core))
        .taskValue,
    },
    Compile / sourceGenerators += {
      sourceDirectory
        .zip(scalaVersion)
        .map { case (sd, sv) =>
          val base = sd.getParentFile()
          sv match {
            case Scala3 =>
              Boilerplate
                .gen(base / "src-3", Boilerplate.BoilerplateModule.Core3)
            case _ =>
              Boilerplate
                .gen(base / "src-2", Boilerplate.BoilerplateModule.Core2)
          }
        }
        .taskValue
    },
    scalacOptions ++= Seq(
      "-Wconf:msg=value noInlineDocumentSupport in class ProtocolDefinition is deprecated:silent"
    ),
    libraryDependencies += Dependencies.collectionsCompat.value,
    Compile / packageSrc / mappings ++= {
      val base = (Compile / sourceManaged).value
      val files = (Compile / managedSources).value
      files
        .map(f => (f, f.relativeTo(base)))
        // this excludes modules/core/src/generated/PartiallyAppliedStruct.scala
        .collect { case (f, Some(relF)) => f -> relF.getPath() }
    },
    scalacOptions ++= Seq(
      "-Wconf:msg=value noInlineDocumentSupport in class ProtocolDefinition is deprecated:silent"
    ),
    mimaBinaryIssueFilters ++= Seq(
      // Incompatible change from smithy 1.46.0
      // Introduced in https://github.com/smithy-lang/smithy/pull/2156
      // Discussed in https://github.com/smithy-lang/smithy/issues/2243
      // Brought to smithy4s in https://github.com/disneystreaming/smithy4s/pull/1485
      ProblemFilters.exclude[MissingClassProblem](
        "smithy.api.TraitChangeSeverity*"
      ),
      ProblemFilters.exclude[IncompatibleMethTypeProblem](
        "smithy.api.TraitDiffRule.apply"
      ),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "smithy.api.TraitDiffRule.<init>$default$2"
      ),
      ProblemFilters.exclude[IncompatibleMethTypeProblem](
        "smithy.api.TraitDiffRule.this"
      ),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "smithy.api.TraitDiffRule.severity"
      ),
      ProblemFilters.exclude[IncompatibleMethTypeProblem](
        "smithy.api.TraitDiffRule.copy"
      ),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "smithy.api.TraitDiffRule.copy$default$2"
      ),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "smithy.api.TraitDiffRule._2"
      ),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "smithy.api.TraitDiffRule.apply$default$2"
      )
    )
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
    libraryDependencies ++= munitDeps.value
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
    libraryDependencies ++= Seq(
      Dependencies.Weaver.cats.value % Test,
      Dependencies.Weaver.scalacheck.value % Test
    ),
    smithy4sDependencies ++= Seq(Dependencies.Smithy.awsTraits),
    Compile / allowedNamespaces := Seq(
      "aws.api",
      "aws.auth",
      "aws.customizations",
      "aws.protocols"
    ),
    genSmithy(Compile),
    Test / envVars ++= Map("TEST_VAR" -> "hello"),
    scalacOptions ++= Seq(
      "-Wconf:msg=class AwsQuery in package (aws\\.)?protocols is deprecated:silent",
      "-Wconf:msg=class Ec2Query in package (aws\\.)?protocols is deprecated:silent",
      "-Wconf:msg=class RestXml in package (aws\\.)?protocols is deprecated:silent",
      "-Wconf:msg=value noErrorWrapping in class RestXml is deprecated:silent"
    )
  )
  .jvmPlatform(allJvmScalaVersions, jvmDimSettings)
  .jsPlatform(
    allJsScalaVersions,
    jsDimSettings ++ Seq(
      Test / jsEnv := new NodeJSEnv(
        NodeJSEnv.Config().withEnv(Map("TEST_VAR" -> "hello"))
      )
    )
  )
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

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
  .dependsOn(
    `aws-kernel`,
    `http4s-kernel`,
    json,
    xml,
    complianceTests % "test->compile",
    dynamic % "test->compile",
    tests % "test->compile",
    testUtils % "test->compile",
    bootstrapped % "test->compile"
  )
  .settings(
    libraryDependencies ++= {
      Seq(
        Dependencies.Fs2.io.value,
        Dependencies.Http4s.client.value,
        Dependencies.Http4s.emberClient.value % Test,
        Dependencies.Weaver.cats.value % Test,
        Dependencies.Weaver.scalacheck.value % Test
      )
    },
    scalacOptions ++= Seq(
      "-Wconf:msg=class AwsQuery in package (aws\\.)?protocols is deprecated:silent",
      "-Wconf:msg=class Ec2Query in package (aws\\.)?protocols is deprecated:silent",
      "-Wconf:msg=class RestXml in package (aws\\.)?protocols is deprecated:silent",
      "-Wconf:msg=value noErrorWrapping in class RestXml is deprecated:silent"
    ),
    Test / complianceTestDependencies := Seq(
      Dependencies.Smithy.`aws-protocol-tests`
    ),
    (Test / resourceGenerators) := Seq(dumpModel(Test).taskValue),
    (Test / smithy4sModelTransformers) := Seq.empty,
    (Test / envVars) ++= {
      val files: Seq[File] =
        (Test / resourceGenerators) {
          _.join.map(_.flatten)
        }.value
      files.headOption
        .map { file =>
          Map(
            "MODEL_DUMP" -> file.getAbsolutePath,
            "AWS_ACCESS_KEY_ID" -> "TEST_KEY",
            "AWS_SECRET_ACCESS_KEY" -> "TEST_SECRET"
          )
        }
        .getOrElse(Map.empty)
    }
  )
  .jvmPlatform(
    latest2ScalaVersions,
    jvmDimSettings ++ Seq(
      libraryDependencies ++= Seq(
        "software.amazon.awssdk" % "aws-core" % "2.20.49" % Test
      )
    )
  )
  .jsPlatform(latest2ScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

/**
 * This module contains the logic used at build time for reading smithy
 * models and rendering Scala (or openapi) code.
 */
lazy val codegen = projectMatrix
  .in(file("modules/codegen"))
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(protocol)
  .jvmPlatform(buildtimejvmScala2Versions, jvmDimSettings)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      scalaBinaryVersion,
      "smithyOrg" -> Dependencies.Smithy.org,
      "smithyVersion" -> Dependencies.Smithy.smithyVersion,
      "alloyOrg" -> Dependencies.Alloy.org,
      "alloyVersion" -> Dependencies.Alloy.alloyVersion
    ),
    buildInfoPackage := "smithy4s.codegen",
    libraryDependencies ++= Seq(
      Dependencies.Cats.core.value,
      Dependencies.Smithy.model,
      Dependencies.Smithy.build,
      Dependencies.Alloy.core,
      Dependencies.Alloy.openapi,
      Dependencies.Smithytranslate.proto,
      "com.lihaoyi" %% "os-lib" % "0.10.1",
      Dependencies.Circe.core.value,
      Dependencies.Circe.parser.value,
      Dependencies.Circe.generic.value,
      Dependencies.collectionsCompat.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "io.get-coursier" %% "coursier" % "2.1.9"
    ),
    libraryDependencies ++= munitDeps.value,
    scalacOptions := scalacOptions.value
      .filterNot(Seq("-Ywarn-value-discard", "-Wvalue-discard").contains),
    bloopEnabled := true,
    Compile / sourceGenerators += {
      sourceManaged
        .map(AwsBoilerplate.generate(_))
        .taskValue,
    }
  )

/**
 * This module is the command-line-interface to the codegen module, that
 * can be used independently of build tools (or that build tools can choose
 * to delegate to in order to implement plugins)
 */
lazy val `codegen-cli` = projectMatrix
  .in(file("modules/codegen-cli"))
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(codegen)
  .jvmPlatform(List(Scala213), jvmDimSettings)
  .settings(
    buildInfoPackage := "smithy4s.codegen.cli",
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
        Seq("-Xmx1G", "-Dplugin.version=" + version.value)
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
        (`aws-kernel`.jvm(Scala213) / publishLocal).value,
        (core.jvm(Scala213) / publishLocal).value,
        (core.jvm(Scala3) / publishLocal).value,
        (dynamic.jvm(Scala213) / publishLocal).value,
        (codegen.jvm(Scala213) / publishLocal).value,
        (core.jvm(Scala3) / publishLocal).value,

        // for sbt
        (codegen.jvm(Scala212) / publishLocal).value,
        (protocol.jvm(autoScalaLibrary = false) / publishLocal).value
      )
      publishLocal.value
    },
    scriptedBufferLog := false,
    bloopEnabled := true
  )

/**
 * Mill plugin to run codegen
 */
lazy val millCodegenPlugin = projectMatrix
  .in(file("modules/mill-codegen-plugin"))
  .jvmPlatform(
    scalaVersions = List(Scala213),
    simpleJVMLayout
  )
  .settings(
    name := "mill-codegen-plugin",
    crossVersion := CrossVersion
      .binaryWith(s"mill${millPlatform(Dependencies.Mill.millVersion)}_", ""),
    libraryDependencies ++= Seq(
      Dependencies.Mill.main,
      Dependencies.Mill.mainApi,
      Dependencies.Mill.scalalib,
      Dependencies.Mill.mainTestkit
    ),
    libraryDependencySchemes += "com.lihaoyi" %% "geny" % VersionScheme.Always,
    publishLocal := {
      // make sure that core and codegen are published before the
      // plugin is published
      // this allows running `scripted` alone
      val _ = List(
        // for the code being built
        (`aws-kernel`.jvm(Scala213) / publishLocal).value,
        (core.jvm(Scala213) / publishLocal).value,
        (core.jvm(Scala3) / publishLocal).value,
        (dynamic.jvm(Scala213) / publishLocal).value,
        (codegen.jvm(Scala213) / publishLocal).value,

        // for mill
        (protocol.jvm(autoScalaLibrary = false) / publishLocal).value
      )
      publishLocal.value
    },
    Test / test := (Test / test).dependsOn(publishLocal).value,
    libraryDependencies ++= munitDeps.value
  )
  .dependsOn(codegen)

lazy val decline = (projectMatrix in file("modules/decline"))
  .settings(
    isMimaEnabled := true,
    name := "decline",
    libraryDependencies ++= List(
      Dependencies.Cats.core.value,
      Dependencies.CatsEffect3.value,
      Dependencies.Decline.core.value,
      Dependencies.Weaver.cats.value % Test
    )
  )
  .dependsOn(
    json,
    bootstrapped % "test->test"
  )
  .jvmPlatform(allJvmScalaVersions, jvmDimSettings)
  .jsPlatform(allJsScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

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
    libraryDependencies ++= Seq(
      Dependencies.Weaver.cats.value % Test,
      Dependencies.Weaver.scalacheck.value % Test
    )
  )
  .settings(Smithy4sBuildPlugin.doNotPublishArtifact)

/**
 * This modules contains utilities to dynamically instantiate
 * the interfaces provide by smithy4s, based on data from dynamic
 * Model instances.
 */
lazy val dynamic = projectMatrix
  .in(file("modules/dynamic"))
  .dependsOn(
    core,
    testUtils % "test->compile",
    bootstrapped % "test->test;test->compile"
  )
  .settings(
    libraryDependencies ++= munitDeps.value ++ Seq(
      Dependencies.collectionsCompat.value,
      Dependencies.Cats.core.value,
      Dependencies.Alloy.core % Test
    ),
    Compile / allowedNamespaces := Seq("smithy4s.dynamic.model"),
    Compile / smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "modules" / "dynamic" / "smithy" / "dynamic.smithy"
    ),
    Test / unmanagedClasspath ++= Seq(
      (ThisBuild / baseDirectory).value / "sampleSpecs"
    ),
    Compile / sourceGenerators := Seq(genSmithyScala(Compile).taskValue),
    Compile / packageSrc / mappings ++= {
      val base = (Compile / sourceManaged).value
      val files = (Compile / managedSources).value
      files
        .map(f => (f, f.relativeTo(base)))
        .collect { case (f, Some(relF)) => f -> relF.getPath() }
    }
  )
  .jvmPlatform(
    allJvmScalaVersions,
    jvmDimSettings ++ Seq(
      libraryDependencies ++= Seq(
        Dependencies.Smithy.model,
        Dependencies.Smithy.diff % Test,
        Dependencies.Smithy.build % Test
      )
    )
  )
  .jsPlatform(allJsScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

/**
 * Module that contains jsoniter-based encoders/decoders for the generated
 * types.
 */
lazy val json = projectMatrix
  .in(file("modules/json"))
  .dependsOn(
    core,
    bootstrapped % "test->test",
    scalacheck % "test -> compile"
  )
  .settings(
    isMimaEnabled := true,
    libraryDependencies ++= Seq(
      Dependencies.Jsoniter.core.value
    ),
    libraryDependencies ++= munitDeps.value
  )
  .jvmPlatform(allJvmScalaVersions, jvmDimSettings)
  .jsPlatform(allJsScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

/**
 * Module that contains fs2-data-based XML encoders/decoders for the generated
 * types.
 */
lazy val xml = projectMatrix
  .in(file("modules/xml"))
  .dependsOn(
    core,
    fs2,
    bootstrapped % "test->test",
    scalacheck % "test -> compile"
  )
  .settings(
    isMimaEnabled := false,
    libraryDependencies ++= Seq(
      Dependencies.Fs2Data.xml.value,
      Dependencies.Weaver.cats.value % Test
    ),
    libraryDependencies ++= munitDeps.value,
    Test / fork := virtualAxes.value.contains(VirtualAxis.jvm)
  )
  .jvmPlatform(allJvmScalaVersions, jvmDimSettings)
  .jsPlatform(allJsScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

/**
 * Module that contains protobuf encoders/decoders for the generated
 * types.
 */
lazy val protobuf = projectMatrix
  .in(file("modules/protobuf"))
  .dependsOn(
    core,
    bootstrapped % "test->test",
    scalacheck % "test -> compile"
  )
  .settings(
    isMimaEnabled := false,
    libraryDependencies ++= munitDeps.value,
    libraryDependencies ++= {
      if (virtualAxes.value.contains(VirtualAxis.jvm))
        Seq(
          "com.google.protobuf" % "protobuf-java" % "3.24.4",
          "com.google.protobuf" % "protobuf-java-util" % "3.24.4" % Test
        )
      else
        Seq(
          "com.thesamet.scalapb" %% "protobuf-runtime-scala" % "0.8.14"
        )
    },
    Test / fork := virtualAxes.value.contains(VirtualAxis.jvm)
  )
  .jvmPlatform(allJvmScalaVersions, jvmDimSettings)
  .jsPlatform(allJsScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

/**
 * Module that contains common code which relies on fs2.
 */
lazy val fs2 = projectMatrix
  .in(file("modules/fs2"))
  .dependsOn(
    core
  )
  .settings(
    isMimaEnabled := false,
    libraryDependencies ++= Seq(
      Dependencies.Fs2.core.value,
      Dependencies.Weaver.cats.value % Test
    ),
    libraryDependencies ++= munitDeps.value,
    Test / fork := virtualAxes.value.contains(VirtualAxis.jvm)
  )
  .jvmPlatform(allJvmScalaVersions, jvmDimSettings)
  .jsPlatform(allJsScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

/**
 * Module that contains an http4s-specific `EntityCompiler` construct
 * that codifies the compilation of smithy4s Schemas to EntityEncoders and
 * EntityDecoders
 */
lazy val `http4s-kernel` = projectMatrix
  .in(file("modules/http4s-kernel"))
  .dependsOn(core, cats)
  .settings(
    isMimaEnabled := true,
    libraryDependencies ++= Seq(
      Dependencies.Http4s.core.value,
      Dependencies.Weaver.cats.value % Test
    )
  )
  .http4sPlatform(allJvmScalaVersions, jvmDimSettings)

/**
 * Module that contains http4s-specific client/server bindings for the
 * custom protocols provided by smithy4s.
 */
lazy val http4s = projectMatrix
  .in(file("modules/http4s"))
  .dependsOn(
    `http4s-kernel`,
    json,
    fs2,
    bootstrapped % "test->compile",
    complianceTests % "test->compile",
    dynamic % "test->compile",
    tests % "test->compile",
    testUtils % "test->compile"
  )
  .settings(
    isMimaEnabled := true,
    libraryDependencies ++= {
      Seq(
        Dependencies.Http4s.core.value,
        Dependencies.Http4s.dsl.value,
        Dependencies.Http4s.client.value,
        Dependencies.Alloy.core % Test,
        Dependencies.Smithy.build % Test,
        Dependencies.Http4s.circe.value % Test,
        Dependencies.Weaver.cats.value % Test,
        Dependencies.Http4s.emberClient.value % Test,
        Dependencies.Http4s.emberServer.value % Test,
        Dependencies.Alloy.`protocol-tests` % Test
      )
    },
    Test / allowedNamespaces := Seq(
      "smithy4s.example.guides.auth"
    ),
    Test / complianceTestDependencies := Seq(
      Dependencies.Alloy.`protocol-tests`
    ),
    (Test / smithy4sModelTransformers) := Seq("ProtocolTransformer"),
    (Test / resourceGenerators) := Seq(dumpModel(Test).taskValue),
    (Test / envVars) := {
      val files: Seq[File] =
        (Test / resourceGenerators) {
          _.join.map(_.flatten)
        }.value
      files.headOption
        .map { file =>
          Map("MODEL_DUMP" -> file.getAbsolutePath)
        }
        .getOrElse(Map.empty)
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
    libraryDependencies ++= {
      Seq(
        Dependencies.Weaver.cats.value % Test,
        Dependencies.Webjars.swaggerUi,
        Dependencies.Webjars.webjarsLocator
      )
    }
  )
  .http4sJvmPlatform(allJvmScalaVersions, jvmDimSettings)

lazy val cats = projectMatrix
  .in(file("modules/cats"))
  .dependsOn(core)
  .settings(
    isMimaEnabled := true,
    libraryDependencies ++= Seq(
      Dependencies.Weaver.cats.value % Test,
      Dependencies.Cats.core.value
    )
  )
  .jvmPlatform(allJvmScalaVersions, jvmDimSettings)
  .jsPlatform(allJsScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

lazy val testUtils = projectMatrix
  .in(file("modules/test-utils"))
  .dependsOn(core)
  .settings(Smithy4sBuildPlugin.doNotPublishArtifact)
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
  .dependsOn(core, complianceTests, dynamic)
  .settings(
    libraryDependencies ++= {
      Seq(
        Dependencies.Http4s.core.value,
        Dependencies.Http4s.dsl.value,
        Dependencies.Http4s.client.value,
        Dependencies.Http4s.circe.value,
        Dependencies.Weaver.cats.value
      )
    },
    Compile / allowedNamespaces := Seq("smithy4s.example"),
    Compile / smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "pizza.smithy"
    ),
    Compile / sourceGenerators := Seq(genSmithyScala(Compile).taskValue)
  )
  .http4sPlatform(allJvmScalaVersions, jvmDimSettings)

lazy val transformers = projectMatrix
  .in(file("modules/transformers"))
  .settings(Smithy4sBuildPlugin.doNotPublishArtifact)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Smithy.model,
      Dependencies.Smithy.build,
      Dependencies.Smithy.testTraits,
      Dependencies.Smithy.awsTraits,
      Dependencies.Alloy.core
    )
  )
  .jvmPlatform(allJvmScalaVersions, jvmDimSettings)
  .jsPlatform(allJsScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

lazy val complianceTests = projectMatrix
  .in(file("modules/compliance-tests"))
  .dependsOn(core)
  .settings(
    name := "compliance-tests",
    Compile / allowedNamespaces := Seq("smithy.test"),
    Compile / smithy4sDependencies ++= Seq(Dependencies.Smithy.testTraits),
    Compile / sourceGenerators := Seq(genSmithyScala(Compile).taskValue),
    libraryDependencies ++= {
      Seq(
        Dependencies.Circe.parser.value,
        Dependencies.Http4s.circe.value,
        Dependencies.Http4s.client.value,
        Dependencies.Weaver.cats.value % Test,
        Dependencies.Pprint.core.value,
        Dependencies.Fs2Data.xml.value
      )
    }
  )
  .http4sPlatform(allJvmScalaVersions, jvmDimSettings)

lazy val exampleGeneratedOutput =
  settingKey[File]("Output directory where the generated code is going to be.")

lazy val exampleGeneratedResourcesOutput =
  settingKey[File](
    "Output directory where the generated resources are going to be."
  )

/**
  * A project that contains generated code, which can serve as a basis for tests.
  */
lazy val bootstrapped = projectMatrix
  .in(file("modules/bootstrapped"))
  .dependsOn(cats, `aws-kernel`, complianceTests)
  .disablePlugins(ScalafixPlugin)
  .disablePlugins(HeaderPlugin)
  .settings(
    // Setting ScalaPB to generate Scala code from proto files generated by
    // smithy4s
    Compile / PB.generate := {
      // running smithy codegen before scalapb codegen to have the translated proto
      genSmithyResources(Compile).taskValue
      (Compile / PB.generate).value
    },
    Compile / PB.protoSources ++= Seq(
      exampleGeneratedResourcesOutput.value
    ),
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    ),
    Test / fork := virtualAxes.value.contains(VirtualAxis.jvm),
    exampleGeneratedOutput := (ThisBuild / baseDirectory).value / "modules" / "bootstrapped" / "src" / "generated",
    exampleGeneratedResourcesOutput := (Compile / resourceDirectory).value,
    cleanFiles ++= Seq(
      exampleGeneratedOutput.value,
      exampleGeneratedResourcesOutput.value
    ),
    smithy4sDependencies ++= Seq(
      Dependencies.Smithy.testTraits,
      Dependencies.Smithy.awsTraits,
      Dependencies.Smithy.waiters
    ),
    Compile / allowedNamespaces := Seq(
      "com.amazonaws.dynamodb",
      "smithy4s.benchmark",
      "smithy4s.example",
      "smithy4s.example.aws",
      "smithy4s.example.import_test",
      "smithy4s.example.imp",
      "smithy4s.example.error",
      "smithy4s.example.common",
      "smithy4s.example.collision",
      "smithy4s.example.greet",
      "smithy4s.example.guides.auth",
      "smithy4s.example.guides.hello",
      "smithy4s.example.hello",
      "smithy4s.example.test",
      "smithy4s.example.package",
      "smithy4s.example.protobuf",
      "weather",
      "smithy4s.example.product",
      "smithy4s.example.reservedNameOverride"
    ),
    smithySpecs := IO.listFiles(
      (ThisBuild / baseDirectory).value / "sampleSpecs"
    ),
    Compile / resourceDirectory := (ThisBuild / baseDirectory).value / "modules" / "bootstrapped" / "resources",
    libraryDependencies += Dependencies.Http4s.emberServer.value,
    genSmithy(Compile),
    genSmithyOutput := exampleGeneratedOutput.value,
    genSmithyResourcesOutput := exampleGeneratedResourcesOutput.value,
    smithy4sSkip := List("resource"),
    // Ignore deprecation warnings here - it's all generated code, anyway.
    scalacOptions ++= Seq(
      "-Wconf:cat=deprecation:silent"
    ) ++ scala3MigrationOption(scalaVersion.value),
    libraryDependencies ++=
      munitDeps.value ++ Seq(
        Dependencies.Cats.core.value % Test,
        Dependencies.Weaver.cats.value % Test,
        "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
        Dependencies.Alloy.protobuf % "protobuf-src"
      )
  )
  .jvmPlatform(allJvmScalaVersions, jvmDimSettings)
  .jsPlatform(allJsScalaVersions, jsDimSettings)
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)
  .settings(Smithy4sBuildPlugin.doNotPublishArtifact)

lazy val guides = projectMatrix
  .in(file("modules/guides"))
  .dependsOn(http4s)
  .dependsOn(bootstrapped)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Http4s.emberServer.value,
      Dependencies.Http4s.emberClient.value,
      Dependencies.Weaver.cats.value % Test
    )
  )
  .jvmPlatform(Seq(Scala3), jvmDimSettings)
  .settings(Smithy4sBuildPlugin.doNotPublishArtifact)

/**
 * Pretty primitive benchmarks to test that we're not doing anything drastically
 * slow.
 */
lazy val benchmark = projectMatrix
  .in(file("modules/benchmark"))
  .enablePlugins(JmhPlugin)
  .dependsOn(
    http4s % "compile -> compile,test",
    `scalacheck`,
    bootstrapped
  )
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Circe.generic.value
    )
  )
  .jvmPlatform(List(Scala213), jvmDimSettings)
  .settings(Smithy4sBuildPlugin.doNotPublishArtifact)

lazy val `aws-sandbox` = projectMatrix
  .in(file("modules/aws-sandbox"))
  .dependsOn(`aws-http4s`)
  .settings(
    Compile / allowedNamespaces := Seq(
      "com.amazonaws.cloudwatch",
      "com.amazonaws.ec2"
    ),
    genSmithy(Compile),
    // Ignore deprecation warnings here - it's all generated code, anyway.
    scalacOptions ++= Seq(
      "-Wconf:cat=deprecation:silent"
    ),
    smithy4sDependencies ++= Seq(
      "com.disneystreaming.smithy" % "aws-cloudwatch-spec" % "2023.02.10",
      "com.disneystreaming.smithy" % "aws-ec2-spec" % "2023.02.10"
    ),
    libraryDependencies ++= Seq(
      Dependencies.Http4s.emberClient.value,
      Dependencies.Slf4jSimple % Runtime
    ),
    run / fork := true
  )
  .jvmPlatform(List(Scala213), jvmDimSettings)
  .settings(Smithy4sBuildPlugin.doNotPublishArtifact)

def genSmithy(config: Configuration) = Def.settings(
  Seq(
    config / sourceGenerators ++= Seq(genSmithyScala(config).taskValue),
    config / resourceGenerators ++= Seq(genSmithyResources(config).taskValue)
  )
)
def genSmithyScala(config: Configuration) = genSmithyImpl(config).map(_._1)
def genSmithyResources(config: Configuration) = genSmithyImpl(config).map(_._2)

// SBT setting to specify artifacts to be included in the Smithy model for compliance testing
val complianceTestDependencies =
  SettingKey[Seq[ModuleID]]("complianceTestDependencies")

// writes out a json representation of the smithy model pulled from Smithy4s dependencies config
// result is cached using the dependency list as the cache key
def dumpModel(config: Configuration): Def.Initialize[Task[Seq[File]]] =
  Def.task {
    val dumpModelCp = (`codegen-cli`.jvm(
      Smithy4sBuildPlugin.Scala213
    ) / Compile / fullClasspath).value
      .map(_.data)
    val transforms = (config / smithy4sModelTransformers).value
    lazy val modelTransformersCp = (transformers.jvm(
      Smithy4sBuildPlugin.Scala213
    ) / Compile / fullClasspath).value
      .map(_.data)

    val cp = (if (transforms.isEmpty) dumpModelCp
              else dumpModelCp ++ modelTransformersCp)
      .map(_.getAbsolutePath())
      .mkString(":")
    val mc = (`codegen-cli`.jvm(
      Smithy4sBuildPlugin.Scala213
    ) / Compile / mainClass).value.getOrElse(
      throw new Exception("No main class found")
    )

    import sjsonnew._
    import BasicJsonProtocol._
    import sbt.FileInfo
    import sbt.HashFileInfo
    import sbt.io.Hash
    import scala.jdk.CollectionConverters._
    implicit val pathFormat: JsonFormat[File] =
      BasicJsonProtocol.projectFormat[File, HashFileInfo](
        p => {
          if (p.isFile()) FileInfo.hash(p)
          else
            // If the path is a directory, we get the hashes of all files
            // then hash the concatenation of the hash's bytes.
            FileInfo.hash(
              p,
              Hash(
                Files
                  .walk(p.toPath(), 2)
                  .collect(Collectors.toList())
                  .asScala
                  .map(_.toFile())
                  .map(Hash(_))
                  .foldLeft(Array.emptyByteArray)(_ ++ _)
              )
            )
        },
        hash => hash.file
      )
    val s = (config / streams).value

    val args =
      if (transforms.isEmpty) List.empty
      else List("--transformers", transforms.mkString(","))
    val cached =
      Tracked.inputChanged[List[String], Seq[File]](
        s.cacheStoreFactory.make("input")
      ) {
        Function.untupled {
          Tracked
            .lastOutput[(Boolean, List[String]), Seq[File]](
              s.cacheStoreFactory.make("output")
            ) { case ((changed, deps), outputs) =>
              if (changed || outputs.isEmpty) {
                val res =
                  ("java" :: "-cp" :: cp :: mc :: "dump-model" :: deps ::: args).!!
                val file =
                  (config / resourceManaged).value / "compliance-tests.json"
                IO.write(file, res)
                Seq(file)

              } else {
                outputs.getOrElse(Seq.empty)
              }
            }
        }
      }

    val trackedFiles = List(
      "--dependencies",
      (config / complianceTestDependencies).?.value
        .getOrElse(Seq.empty)
        .map { moduleId =>
          s"${moduleId.organization}:${moduleId.name}:${moduleId.revision}"
        }
        .mkString(",")
    )

    cached(trackedFiles)
  }

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
    (config / genSmithyResourcesOutput).?.value
      .getOrElse((config / resourceManaged).value)
      .getAbsolutePath()
  val allowedNS = (config / allowedNamespaces).?.value.filterNot(_.isEmpty)
  val skip = (config / smithy4sSkip).?.value.getOrElse(Seq.empty)
  val smithy4sDeps =
    (config / smithy4sDependencies).?.value.getOrElse(Seq.empty).map {
      moduleId =>
        s"${moduleId.organization}:${moduleId.name}:${moduleId.revision}"
    }

  val codegenCp =
    (`codegen-cli`.jvm(
      Smithy4sBuildPlugin.Scala213
    ) / Compile / fullClasspath).value
      .map(_.data)

  val mc = "smithy4s.codegen.cli.Main"
  val s = (config / streams).value

  import sjsonnew._
  import BasicJsonProtocol._
  import sbt.FileInfo
  import sbt.HashFileInfo
  import sbt.io.Hash
  import scala.jdk.CollectionConverters._

  // Json codecs used by SBT's caching constructs
  // This serialises a path by providing a hash of the content it points to.
  // Because the hash is part of the Json, this allows SBT to detect when a file
  // changes and invalidate its relevant caches, leading to a call to Smithy4s' code generator.
  implicit val pathFormat: JsonFormat[File] =
    BasicJsonProtocol.projectFormat[File, HashFileInfo](
      p => {
        if (p.isFile()) FileInfo.hash(p)
        else
          // If the path is a directory, we get the hashes of all files
          // then hash the concatenation of the hash's bytes.
          FileInfo.hash(
            p,
            Hash(
              Files
                .walk(p.toPath(), 2)
                .collect(Collectors.toList())
                .asScala
                .map(_.toFile())
                .map(Hash(_))
                .foldLeft(Array.emptyByteArray)(_ ++ _)
            )
          )
      },
      hash => hash.file
    )

  case class CodegenInput(files: Seq[File])
  object CodegenInput {
    implicit val seqFormat: JsonFormat[CodegenInput] =
      BasicJsonProtocol.projectFormat[CodegenInput, Seq[File]](
        input => input.files,
        files => CodegenInput(files)
      )(BasicJsonProtocol.seqFormat(pathFormat))
  }

  val cached =
    Tracked.inputChanged[CodegenInput, Seq[File]](
      s.cacheStoreFactory.make("input")
    ) {
      Function.untupled {
        Tracked
          .lastOutput[(Boolean, CodegenInput), Seq[File]](
            s.cacheStoreFactory.make("output")
          ) { case ((changed, files), outputs) =>
            if (changed || outputs.isEmpty) {
              val inputs = inputFiles.map(_.getAbsolutePath()).toList
              val outputOpt = List("--output", outputDir)
              val resourceOutputOpt =
                List("--resource-output", resourceOutputDir)
              val allowedNsOpt =
                if (allowedNS.isDefined)
                  List("--allowed-ns", allowedNS.get.mkString(","))
                else Nil
              val skipOpt = skip.flatMap(s => List("--skip", s))
              val dependenciesOpt =
                if (smithy4sDeps.nonEmpty)
                  List("--dependencies", smithy4sDeps.mkString(","))
                else Nil
              val args = outputOpt ++
                resourceOutputOpt ++
                allowedNsOpt ++
                inputs ++
                skipOpt ++
                dependenciesOpt

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
  cached(CodegenInput(trackedFiles)).partition(_.ext == "scala")
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
