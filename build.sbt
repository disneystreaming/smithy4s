import _root_.java.util.stream.Collectors
import java.nio.file.Files
import sbt.internal.IvyConsole
import org.scalajs.jsenv.nodejs.NodeJSEnv

import java.io.File
import sys.process._

ThisBuild / commands ++= createBuildCommands(allModules)
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"
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
  example,
  tests,
  http4s,
  `http4s-kernel`,
  `http4s-swagger`,
  decline,
  codegenPlugin,
  benchmark,
  protocol,
  protocolTests,
  `aws-kernel`,
  aws,
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
      `aws-http4s` % "compile -> compile,test",
      complianceTests
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
        Dependencies.AwsSpecSummary.value
      ),
      Compile / smithy4sDependencies ++= Seq(Dependencies.Smithy.testTraits),
      Compile / sourceGenerators := Seq(genSmithyScala(Compile).taskValue),
      Compile / smithySpecs := Seq(
        (Compile / sourceDirectory).value / "smithy",
        (ThisBuild / baseDirectory).value / "sampleSpecs" / "test.smithy",
        (ThisBuild / baseDirectory).value / "modules" / "guides" / "smithy" / "auth.smithy",
        (ThisBuild / baseDirectory).value / "sampleSpecs" / "hello.smithy",
        (ThisBuild / baseDirectory).value / "sampleSpecs" / "kvstore.smithy"
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
      "alloy.common"
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
    libraryDependencies ++= Seq(
      Dependencies.collectionsCompat.value,
      Dependencies.Cats.core.value % Test
    ),
    libraryDependencies ++= munitDeps.value,
    Test / allowedNamespaces := Seq(
      "smithy4s.example",
      "smithy4s.example.collision"
    ),
    Test / smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "adtMember.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "bodies.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "defaults.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "discriminated.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "enums.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "errorHandling.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "errors.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "example.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "kvstore.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "metadata.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "misc.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "namecollision.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "packedInputs.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "product.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "recursive.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "reservednames.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "resources.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "untagged.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "weather.smithy"
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
      "-Wconf:msg=class RestXml in package aws.protocols is deprecated:silent",
      "-Wconf:msg=value noErrorWrapping in class RestXml is deprecated:silent",
      "-Wconf:msg=class Ec2Query in package aws.protocols is deprecated:silent"
    )
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
  .nativePlatform(allNativeScalaVersions, nativeDimSettings)

/**
 * cats-effect specific abstractions of AWS protocol interpreters and constructs
 */
lazy val aws = projectMatrix
  .in(file("modules/aws"))
  .dependsOn(`aws-kernel`, json, xml)
  .settings(
    libraryDependencies ++= {
      // Only building this module against CE3
      Seq(
        Dependencies.Fs2.core.value,
        Dependencies.Fs2.io.value,
        Dependencies.Weaver.cats.value % Test,
        Dependencies.Weaver.scalacheck.value % Test
      )
    },
    Test / smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "aws_example.smithy"
    ),
    Test / sourceGenerators := Seq(genSmithyScala(Test).taskValue),
    Test / smithy4sDependencies ++= Seq(
      Dependencies.Smithy.awsTraits
    ),
    scalacOptions ++= Seq(
      "-Wconf:msg=class AwsQuery in package (aws\\.)?protocols is deprecated:silent"
    )
  )
  .jvmPlatform(latest2ScalaVersions, jvmDimSettings)
  .jsPlatform(latest2ScalaVersions, jsDimSettings)
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
  .dependsOn(aws, complianceTests % "test->compile", dynamic % "test->compile")
  .settings(
    libraryDependencies ++= {
      Seq(
        Dependencies.Http4s.client.value,
        Dependencies.Http4s.emberClient.value % Test
      )
    },
    Test / smithy4sDependencies ++= Seq(
      Dependencies.Smithy.waiters,
      Dependencies.Smithy.awsTraits
    ),
    Test / allowedNamespaces := Seq("com.amazonaws.dynamodb"),
    Test / sourceGenerators := Seq(genSmithyScala(Test).taskValue)
  )
  .jvmPlatform(
    latest2ScalaVersions,
    jvmDimSettings ++ Seq(
      Test / smithySpecs ++= Seq(
        (ThisBuild / baseDirectory).value / "sampleSpecs" / "dynamodb.2012-08-10.json",
        (ThisBuild / baseDirectory).value / "sampleSpecs" / "lambda.json"
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
      "com.lihaoyi" %% "os-lib" % "0.8.1",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.2.0",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "io.get-coursier" %% "coursier" % "2.1.2"
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
    scriptedBufferLog := false
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
  .dependsOn(json)
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
  .dependsOn(core % "test->test;compile->compile", testUtils % "test->compile")
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.10.0",
      Dependencies.Cats.core.value
    ),
    libraryDependencies ++= List
      .concat(munitDeps.value, List(Dependencies.Alloy.core % Test)),
    Compile / allowedNamespaces := Seq("smithy4s.dynamic.model"),
    Compile / smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "modules" / "dynamic" / "smithy" / "dynamic.smithy"
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
      libraryDependencies += Dependencies.Smithy.model
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
    core % "test->test;compile->compile",
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
    core % "test->test;compile->compile",
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
 * Module that contains an http4s-specific `EntityCompiler` construct
 * that codifies the compilation of smithy4s Schemas to EntityEncoders and
 * EntityDecoders
 */
lazy val `http4s-kernel` = projectMatrix
  .in(file("modules/http4s-kernel"))
  .dependsOn(core)
  .settings(
    isMimaEnabled := true,
    libraryDependencies ++= Seq(
      Dependencies.Http4s.core.value
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
    Test / allowedNamespaces := Seq("smithy4s.hello"),
    Test / smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "hello.smithy"
    ),
    Test / smithy4sSkip := Seq("openapi"),
    (Test / sourceGenerators) := Seq(genSmithyScala(Test).taskValue),
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
  .dependsOn(core)
  .settings(
    allowedNamespaces := Seq(
      "smithy4s.example"
    ),
    libraryDependencies ++= {
      Seq(
        Dependencies.Http4s.core.value,
        Dependencies.Http4s.dsl.value,
        Dependencies.Http4s.client.value,
        Dependencies.Http4s.circe.value,
        Dependencies.Weaver.cats.value
      )
    },
    Compile / smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "pizza.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "weather.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "recursiveInput.smithy"
    ),
    (Compile / sourceGenerators) := Seq(genSmithyScala(Compile).taskValue)
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
    Compile / allowedNamespaces := Seq("smithy.test", "smithy4s.example.test"),
    Compile / smithy4sDependencies ++= Seq(Dependencies.Smithy.testTraits),
    Compile / sourceGenerators := Seq(genSmithyScala(Compile).taskValue),
    libraryDependencies ++= {
      Seq(
        Dependencies.Circe.parser.value,
        Dependencies.Http4s.circe.value,
        Dependencies.Http4s.client.value,
        Dependencies.Weaver.cats.value % Test,
        Dependencies.Pprint.core.value
      )
    }
  )
  .http4sPlatform(allJvmScalaVersions, jvmDimSettings)

/**
 * Example application using the custom REST-JSON protocol provided by
 * smithy4s.
 *
 * (almost) all Scala code in this module is generated! The ones that aren't should have a note stating so.
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
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "deprecations.smithy",
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
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "defaults.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "quoted_string.smithy",
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "numeric.smithy"
    ),
    Compile / resourceDirectory := (ThisBuild / baseDirectory).value / "modules" / "example" / "resources",
    libraryDependencies += Dependencies.Http4s.emberServer.value,
    genSmithy(Compile),
    genSmithyOutput := ((ThisBuild / baseDirectory).value / "modules" / "example" / "src"),
    genSmithyResourcesOutput := (Compile / resourceDirectory).value,
    smithy4sSkip := List("resource"),
    // Ignore deprecation warnings here - it's all generated code, anyway.
    scalacOptions ++= Seq(
      "-Wconf:cat=deprecation:silent"
    ) ++ scala3MigrationOption(scalaVersion.value)
  )
  .jvmPlatform(latest2ScalaVersions, jvmDimSettings)
  .settings(Smithy4sBuildPlugin.doNotPublishArtifact)

lazy val guides = projectMatrix
  .in(file("modules/guides"))
  .dependsOn(http4s)
  .settings(
    Compile / allowedNamespaces := Seq(
      "smithy4s.guides.hello",
      "smithy4s.guides.auth"
    ),
    smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "modules" / "guides" / "smithy" / "hello.smithy",
      (ThisBuild / baseDirectory).value / "modules" / "guides" / "smithy" / "auth.smithy"
    ),
    genSmithy(Compile),
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
    `scalacheck`
  )
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Circe.generic.value
    ),
    smithySpecs := Seq(
      (ThisBuild / baseDirectory).value / "sampleSpecs" / "benchmark.smithy"
    ),
    genSmithy(Compile)
  )
  .jvmPlatform(List(Scala213), jvmDimSettings)
  .settings(Smithy4sBuildPlugin.doNotPublishArtifact)

def genSmithy(config: Configuration) = Def.settings(
  Seq(
    config / sourceGenerators := Seq(genSmithyScala(config).taskValue),
    config / resourceGenerators := Seq(genSmithyResources(config).taskValue)
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
    val modelTransformersCp = (transformers.jvm(
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
