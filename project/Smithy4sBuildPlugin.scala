import scalafix.sbt.ScalafixPlugin.autoImport._
import xerial.sbt.Sonatype.SonatypeKeys._
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._

import sbt._
import sbt.Keys._
import com.jsuereth.sbtpgp.PgpKeys._
import sbt.internal.LogManager
import sbt.internal.util.BufferedAppender
import java.io.PrintStream
import sbt.internal.ProjectMatrix
import sbtprojectmatrix.ProjectMatrixPlugin.autoImport.virtualAxes
import org.scalajs.sbtplugin.ScalaJSPlugin
import scala.scalanative.sbtplugin.ScalaNativePlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.scalaJSLinkerConfig
import org.scalajs.linker.interface.ModuleKind
import org.scalajs.jsenv.nodejs.NodeJSEnv
import com.github.sbt.git.SbtGit.git

sealed trait Platform
case object JSPlatform extends Platform
case object NativePlatform extends Platform
case object JVMPlatform extends Platform

case class CatsEffectAxis(idSuffix: String, directorySuffix: String)
    extends VirtualAxis.WeakAxis

object Smithy4sBuildPlugin extends AutoPlugin {
  val CatsEffect3Axis = CatsEffectAxis("_CE3", "ce3")
  val CatsEffect2Axis = CatsEffectAxis("_CE2", "ce2")

  val Scala212 = "2.12.17"
  val Scala213 = "2.13.10"
  val Scala3 = "3.2.1"

  object autoImport {
    // format: off
    val smithySpecs              = SettingKey[Seq[File]]("smithySpecs")
    val genSmithyOutput          = SettingKey[File]("genSmithyOutput")
    val genSmithyResourcesOutput = SettingKey[File]("genSmithyResourcesOutput")
    val allowedNamespaces        = SettingKey[Seq[String]]("allowedNamespaces")
    val smithy4sModelTransformers = SettingKey[Seq[String]]("smithy4sModelTransformers")
    val smithy4sDependencies     = SettingKey[Seq[ModuleID]]("smithy4sDependencies")
    val smithy4sSkip             = SettingKey[Seq[String]]("smithy4sSkip")
    val isCE3 = settingKey[Boolean]("Is the current build using CE3?")
    // format: on
  }
  import autoImport._

  implicit class ProjectMatrixOps(val pm: ProjectMatrix) extends AnyVal {
    def http4sJvmPlatform(
        scalaVersions: Seq[String],
        settings: Seq[Setting[_]]
    ) = {
      pm
        .defaultAxes(
          VirtualAxis.jvm,
          VirtualAxis.scalaPartialVersion(Scala213),
          CatsEffect3Axis
        )
        .customRow(
          scalaVersions = scalaVersions,
          axisValues = Seq(VirtualAxis.jvm, CatsEffect3Axis),
          settings = settings
        )
        .customRow(
          scalaVersions = scalaVersions.filterNot(_.startsWith("3")),
          axisValues = Seq(VirtualAxis.jvm, CatsEffect2Axis),
          settings = settings
        )
    }

    def http4sPlatform(
        scalaVersions: Seq[String],
        settings: Seq[Setting[_]]
    ) = {
      http4sJvmPlatform(scalaVersions, settings)
        .customRow(
          scalaVersions = scalaVersions.filterNot(_.startsWith("2.12")),
          axisValues = Seq(VirtualAxis.js, CatsEffect3Axis),
          configureScalaJSProject(_)
        )
        .customRow(
          scalaVersions = scalaVersions.filter(_.startsWith("3")),
          axisValues = Seq(VirtualAxis.native, CatsEffect3Axis),
          _.enablePlugins(ScalaNativePlugin).settings(simpleNativeLayout)
        )
    }
  }

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  override def buildSettings: Seq[Setting[_]] = Seq(
    smithySpecs := Seq.empty,
    smithy4sDependencies := Seq(Dependencies.Alloy.core)
  )

  override val globalSettings = Seq(
    excludeLintKeys ++= Set(
      logManager,
      publishMavenStyle
    )
  )

  /** @see [[sbt.AutoPlugin]] */
  override val projectSettings = Seq(
    moduleName := s"smithy4s-${name.value}",
    scalacOptions ++= compilerOptions(scalaVersion.value),
    // Turning off fatal warnings for ScalaDoc, otherwise we can't release.
    Compile / doc / scalacOptions ~= (_ filterNot (_ == "-Xfatal-warnings")),
    // ScalaDoc settings
    autoAPIMappings := true,
    // ThisBuild / scalacOptions ++= Seq(
    //   // Note, this is used by the doc-source-url feature to determine the
    //   // relative path of a given source file. If it's not a prefix of a the
    //   // absolute path of the source file, the absolute path of that file
    //   // will be put into the FILE_SOURCE variable, which is
    //   // definitely not what we want.
    //   "-sourcepath",
    //   file(".").getAbsolutePath.replaceAll("[.]$", "")
    // ),
    // https://github.com/sbt/sbt/issues/2654
    incOptions := incOptions.value.withLogRecompileOnMacro(false),
    // https://scalacenter.github.io/scalafix/docs/users/installation.html
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    Test / fork := virtualAxes.?.value.forall(_.contains(VirtualAxis.jvm)),
    Test / javaOptions += s"-Duser.dir=${sys.props("user.dir")}",
    Compile / packageBin / packageOptions += {
      // This piece of logic aims at tracking the dependencies that Smithy4s used to generate
      // code at build time, in the manifest of the jar. This helps automatically pulling
      // the corresponding jars and prevents the users from having to search
      import java.util.jar.Manifest
      val manifest = new Manifest
      val scalaBin = scalaBinaryVersion.?.value
      val maybeDeps = smithy4sDependencies.?.value.map {
        _.flatMap(moduleIdEncode(_, scalaBin))
          .mkString(",")
      }
      maybeDeps.foreach { deps =>
        manifest
          .getMainAttributes()
          .put(new java.util.jar.Attributes.Name("smithy4sDependencies"), deps)
      }
      Package.JarManifest(manifest)
    },
    // Ignores warnings in code using the deprecated Enum trait.
    scalacOptions ++= Seq(
      "-Wconf:msg=object Enum in package api is deprecated:silent",
      "-Wconf:msg=type Enum in package api is deprecated:silent",
      // for Scala 3
      "-Wconf:msg=object Enum in package smithy.api is deprecated:silent",
      "-Wconf:msg=type Enum in package smithy.api is deprecated:silent"
    )
  ) ++ publishSettings ++ loggingSettings ++ compilerPlugins ++ headerSettings

  lazy val compilerPlugins = Seq(
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("2."))
        Seq(
          compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
          compilerPlugin(
            "org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full
          )
        )
      else Seq.empty
    }
  )

  lazy val loggingSettings = Seq(
    logManager := LogManager.defaultManager(
      ConsoleOut.printStreamOut(new PrintStream(System.out) {
        val project = thisProjectRef.value.project

        override def println(str: String): Unit = {
          val (lvl, msg) = str.span(_ != ']')
          super.println(s"$lvl] [$project$msg")
        }
      })
    )
  )

  def artifactName(nm: String, axes: Seq[VirtualAxis]) = {
    nm + axes
      .sortBy[Int] {
        case _: VirtualAxis.ScalaVersionAxis => 0
        case _: VirtualAxis.PlatformAxis     => 1
        case _: VirtualAxis.StrongAxis       => 2
        case _: VirtualAxis.WeakAxis         => 3
      }
      .map(_.idSuffix)
      .mkString("-", "-", "")
  }

  lazy val remoteCacheSettings = Seq(
    pushRemoteCacheTo := Some(
      MavenCache("local-cache", file("/tmp/remote-cache"))
    ),
    Compile / packageCache / moduleName := artifactName(
      moduleName.value,
      virtualAxes.value
    )
  )

  def compilerOptions(scalaVersion: String) = {
    val base =
      if (scalaVersion.startsWith("3."))
        filterScala3Options(commonCompilerOptions)
      else
        commonCompilerOptions
    // ++ Seq(
    //   "-Xsource:3",
    //   "-P:kind-projector:underscore-placeholders"
    // )

    base ++ targetScalacOptions(scalaVersion) ++ {
      if (priorTo2_13(scalaVersion)) compilerOptions2_12_Only
      else Seq.empty
    }
  }

  def targetScalacOptions(scalaVersion: String) =
    if (scalaVersion.startsWith("2.12")) Seq("-target:jvm-1.8", "-release", "8")
    else if (scalaVersion.startsWith("2.13")) Seq("-release", "8")
    else if (scalaVersion.startsWith("3.")) Seq("-release", "8")
    else Seq.empty // when we get Scala 4...

  def filterScala3Options(opts: Seq[String]) =
    ("-Ykind-projector" +: opts)
      .filterNot(_.startsWith("-Xlint"))
      .filterNot(_.startsWith("-Ywarn-"))
      .filterNot(_ == "-explaintypes")
      .filterNot(_ == "-Xcheckinit")

  def priorTo2_13(scalaVersion: String): Boolean =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, minor)) if minor < 13 => true
      case _                              => false
    }

  lazy val commonCompilerOptions = Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "utf-8", // Specify character encoding used by source files.
    "-explaintypes", // Explain type errors in more detail.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-language:higherKinds", // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
    "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
    "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
    "-Xlint:option-implicit", // Option.apply used implicit view.
    "-Xlint:package-object-classes", // Class or object defined in package object.
    "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
    "-Ywarn-numeric-widen", // Warn when numerics are widened.
    "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals", // Warn if a local definition is unused.
    "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates", // Warn if a private member is unused.
    "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
    "-Xfatal-warnings" // Fail the compilation if there are any warnings.
  )

  lazy val compilerOptions2_12_Only =
    // These are unrecognized for Scala 2.13.
    Seq(
      "-Xfuture", // Turn on future language features.
      "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
      "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Xlint:unsound-match", // Pattern match may not be typesafe.
      "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
      "-Ypartial-unification", // Enable partial unification in type constructor inference
      "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
      "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
      "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Ywarn-nullary-unit" // Warn when nullary methods return Unit.
    )

  lazy val doNotPublishArtifact = Seq(
    publish / skip := true,
    publish := {},
    publishArtifact := false,
    Compile / packageDoc / publishArtifact := false,
    Compile / packageSrc / publishArtifact := false,
    Compile / packageBin / publishArtifact := false
  )

  lazy val headerSettings = Seq(
    headerLicense := Some(
      HeaderLicense.Custom(
        """| Copyright 2021-2022 Disney Streaming
           |
           | Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
           | you may not use this file except in compliance with the License.
           | You may obtain a copy of the License at
           |
           |    https://disneystreaming.github.io/TOST-1.0.txt
           |
           | Unless required by applicable law or agreed to in writing, software
           | distributed under the License is distributed on an "AS IS" BASIS,
           | WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
           | See the License for the specific language governing permissions and
           | limitations under the License.
           |""".stripMargin
      )
    )
  )

  // Mill-like simple layout
  def simpleLayout(
      platform: Platform,
      catsEffect: Boolean = false
  ): Seq[Setting[_]] = {

    val baseDir = Def.setting {
      sourceDirectory.value.getParentFile
    }

    val platformSuffix = Def.setting {
      platform match {
        case JVMPlatform    => Seq("-jvm", "-jvm-native", "-jvm-js")
        case JSPlatform     => Seq("-js", "-jvm-js", "-js-native")
        case NativePlatform => Seq("-native", "-jvm-native", "-js-native")
      }
    }

    val scalaVersionSuffix = Def
      .setting {
        scalaBinaryVersion.value match {
          case "2.11" => Seq("-2", "-2.11")
          case "2.12" => Seq("-2", "-2.12")
          case "2.13" => Seq("-2", "-2.13")
          case _      => Seq("-3")
        }
      }

    val catsEffectSuffix = Def.setting {
      val ce = virtualAxes.value.collectFirst { case ax: CatsEffectAxis =>
        ax
      }

      ce match {
        case Some(CatsEffect2Axis) => Seq("-ce2")
        case Some(CatsEffect3Axis) => Seq("-ce3")
        case _                     => Seq.empty
      }
    }

    val crossCompilationDirs = Def.setting {
      val empty = Seq("")

      // god forbid we ever have to put files in these folders
      val crissCross = for {
        platform <- platformSuffix.value ++ empty
        version <- scalaVersionSuffix.value ++ empty
        ce <- catsEffectSuffix.value ++ empty
      } yield s"src$platform$version$ce"
      crissCross
    }

    Seq(
      Compile / unmanagedSourceDirectories := Seq(
        baseDir.value / "src"
      ) ++ crossCompilationDirs.value.map(baseDir.value / _),
      Compile / unmanagedResourceDirectories := Seq(
        baseDir.value / "resources"
      ),
      Test / unmanagedSourceDirectories := Seq(
        baseDir.value / "test" / "src"
      ) ++ crossCompilationDirs.value.map(baseDir.value / "test" / _),
      Test / unmanagedResourceDirectories := Seq(
        baseDir.value / "test" / "resources"
      )
    ) ++ remoteCacheSettings
  }

  lazy val jsDimSettings = simpleJSLayout ++ Seq(
    scalacOptions ++= {
      // Map the sourcemaps to github paths instead of local directories
      val flag =
        if (scalaVersion.value.startsWith("3")) "-scalajs-mapSourceURI"
        else "-P:scalajs:mapSourceURI"
      val localSourcesPath = (LocalRootProject / baseDirectory).value.toURI
      val headCommit = git.gitHeadCommit.value.get
      scmInfo.value.map { info =>
        val remoteSourcesPath =
          s"${info.browseUrl.toString
            .replace("github.com", "raw.githubusercontent.com")}/$headCommit"
        s"${flag}:$localSourcesPath->$remoteSourcesPath"
      }
    },
    Test / scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    },
    Test / fork := false
  ) ++ {
    // on CI, use linker's batch mode:
    // https://github.com/scala-js/scala-js/blob/6622d0b8f99bec4dbe1b29c125d111fdea246d34/linker-interface/shared/src/main/scala/org/scalajs/linker/interface/StandardConfig.scala#L51
    // When you run a lot of linkers in parallel
    // they will retain intermediate state (in case you want incremental compilation)
    // on CI we don't want that
    if (sys.env.contains("CI")) Seq(scalaJSLinkerConfig ~= {
      _.withBatchMode(true)
    })
    else Seq.empty
  }

  lazy val jvmDimSettings = simpleJVMLayout
  lazy val nativeDimSettings = simpleNativeLayout ++ Seq(Test / fork := false)

  lazy val simpleJSLayout = simpleLayout(JSPlatform)
  lazy val simpleJVMLayout = simpleLayout(JVMPlatform)
  lazy val simpleNativeLayout = simpleLayout(NativePlatform)

  lazy val publishSettings = Seq(
    organization := "com.disneystreaming.smithy4s",
    sonatypeProfileName := "com.disneystreaming",
    version := sys.env
      .get("GITHUB_REF")
      .filter(_.startsWith("refs/tags/v"))
      .map(_.drop("refs/tags/v".length))
      .getOrElse(version.value),
    publishTo := sonatypePublishToBundle.value,
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    publishMavenStyle := true,
    publishLocal / publishMavenStyle := false,
    homepage := Some(url("https://github.com/disneystreaming")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/disneystreaming/smithy4s"),
        "scm:git@github.com:disneystreaming/smithy4s.git"
      )
    ),
    developers := List(
      Developer(
        id = "baccata",
        name = "Olivier Mélois",
        email = "baccata64@gmail.com",
        url = url("https://github.com/baccata")
      ),
      Developer(
        id = "keynmol",
        name = "Anton Sviridov",
        email = "keynmol@gmail.com",
        url = url("https://github.com/keynmol")
      ),
      Developer(
        id = "lewisjkl",
        name = "Jeff Lewis",
        email = "lewisjkl@me.com",
        url = url("https://github.com/lewisjkl")
      ),
      Developer(
        id = "kubukoz",
        name = "Jakub Kozłowski",
        email = "kubukoz@gmail.com",
        url = url("https://github.com/kubukoz")
      )
    ),
    credentials ++=
      sys.env
        .get("SONATYPE_USERNAME")
        .zip(sys.env.get("SONATYPE_PASSWORD"))
        .map { case (username, password) =>
          Credentials(
            "Sonatype Nexus Repository Manager",
            "oss.sonatype.org",
            username,
            password
          )
        }
        .toSeq
  )

  def createBuildCommands(projects: Seq[ProjectReference]) = {
    case class Triplet(ce: String, scala: String, platform: String)

    val scala3Suffix = VirtualAxis.scalaABIVersion(Scala3).idSuffix
    val scala213Suffix = VirtualAxis.scalaABIVersion(Scala213).idSuffix
    val scala212Suffix = VirtualAxis.scalaABIVersion(Scala212).idSuffix
    val jsSuffix = VirtualAxis.js.idSuffix
    val nativeSuffix = VirtualAxis.native.idSuffix
    val ce3Suffix = CatsEffect3Axis.idSuffix
    val ce2Suffix = CatsEffect2Axis.idSuffix

    val all: List[(Triplet, Seq[String])] =
      projects
        .collect { case lp: LocalProject =>
          var projectId = lp.project

          val scalaAxis =
            if (
              projectId.endsWith(scala3Suffix) && !projectId.endsWith(ce3Suffix)
            ) {
              projectId = projectId.dropRight(scala3Suffix.length)
              "3_0"
            } else if (projectId.endsWith(scala212Suffix)) {
              projectId = projectId.dropRight(scala212Suffix.length)
              "2_12"
            } else
              "2_13"

          val platformAxis =
            if (projectId.endsWith(jsSuffix)) {
              projectId = projectId.dropRight(jsSuffix.length)
              "js"
            } else if (projectId.endsWith(nativeSuffix)) {
              projectId = projectId.dropRight(nativeSuffix.length)
              "native"
            } else {
              "jvm"
            }

          val ceAxis =
            if (projectId.endsWith(ce2Suffix)) {
              projectId = projectId.dropRight(ce2Suffix.length)
              "CE2"
            } else {
              "default"
            }

          Triplet(ceAxis, scalaAxis, platformAxis) -> lp.project
        }
        .groupBy(_._1)
        .mapValues(_.map(_._2))
        .toList

    // some commands, like test and compile, are setup for all modules
    val any = (t: Triplet) => true
    // things like scalafix and scalafmt are only enabled on jvm 2.13 projects
    val jvm2_13 = (t: Triplet) => t.scala == "2_13" && t.platform == "jvm"

    val jvm = (t: Triplet) => t.platform == "jvm"

    val desiredCommands: Map[String, (String, Triplet => Boolean)] = Map(
      "test" -> ("test", any),
      "compile" -> ("compile", any),
      "publishLocal" -> ("publishLocal", any),
      "pushRemoteCache" -> ("pushRemoteCache", any),
      "scalafix" -> ("scalafix --check", jvm2_13),
      "scalafixTests" -> ("Test/scalafix --check", jvm2_13),
      "scalafmt" -> ("scalafmtCheckAll", jvm2_13),
      "mimaReportBinaryIssuesIfRelevant" -> ("mimaReportBinaryIssuesIfRelevant", jvm)
    )

    val cmds = all.flatMap { case (triplet, projects) =>
      desiredCommands.filter(_._2._2(triplet)).map { case (name, (cmd, _)) =>
        Command.command(
          s"${name}_${triplet.ce}_${triplet.scala}_${triplet.platform}"
        ) { state =>
          projects.foldLeft(state) { case (st, proj) =>
            s"$proj/$cmd" :: st
          }
        }
      }
    }

    cmds
  }

  def configureScalaJSProject(proj: Project): Project = {
    proj
      .enablePlugins(ScalaJSPlugin)
      .settings(jsDimSettings)
  }

  def millPlatform(millVersion: String): String = millVersion match {
    case mv if mv.startsWith("0.10") => "0.10"
    case _                           => sys.error("Unsupported mill platform.")
  }

  private def moduleIdEncode(
      moduleId: ModuleID,
      scalaBinaryVersion: Option[String]
  ): List[String] = {
    (moduleId.crossVersion, scalaBinaryVersion) match {
      case (Disabled, _) =>
        List(s"${moduleId.organization}:${moduleId.name}:${moduleId.revision}")
      case (_: Binary, Some(sbv)) =>
        List(
          s"${moduleId.organization}:${moduleId.name}_${sbv}:${moduleId.revision}"
        )
      case (_, _) => Nil
    }
  }

}
