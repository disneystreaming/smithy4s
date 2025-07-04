package smithy4s.codegen

import munit.FunSuite
import com.typesafe.tools.mima.lib.MiMaLib
import com.typesafe.tools.mima.core.ReversedMissingMethodProblem
import cats.syntax.all._

class BincompatCodegenIntegrationSpec extends FunSuite {
  private val scalaVersions = List("2.12", "2.13", "3")

  private val modelPrefix =
    """$version: "2"
      |namespace demo
      |
      |use smithy4s.meta#bincompatFriendly
      |use smithy4s.meta#bincompatAdded
      |
      |""".stripMargin

  scalaVersions.foreach { scalaVersion =>
    test(s"Bincompat-friendly structs (Scala $scalaVersion)") {
      modelChanges(
        "baseline" ->
          s"""$modelPrefix
             |@bincompatFriendly
             |structure Hello {
             |  @required s1: String
             |  s9: String
             |}
             |""".stripMargin,
        "defaultAdded" ->
          s"""$modelPrefix
             |@bincompatFriendly
             |structure Hello {
             |  @required s1: String = "s1"
             |  s9: String
             |}
             |""".stripMargin,
        "optionalAdded" ->
          s"""$modelPrefix
             |@bincompatFriendly
             |structure Hello {
             |  @required s1: String = "s1"
             |  @bincompatAdded(version: "1.0.0") s2: String
             |  s9: String
             |}
             |""".stripMargin,
        "requiredWithDefaultAdded" ->
          s"""$modelPrefix
             |@bincompatFriendly
             |structure Hello {
             |  @required s1: String = "s1"
             |  @bincompatAdded(version: "1.0.0") s2: String
             |  @bincompatAdded(version: "2.0.0") @required s3: String = "s3Default"
             |  s9: String
             |}
             |""".stripMargin
      )
        .withRunScalaCode(
          s"""|object Main extends App {
              |  val h = demo.Hello("hello s1", Some("hello s9"))
              |  println(h)
              |  assert(h.s1.length == 8)
              |  assert(h.s9.get.length == 8)
              |}
              |""".stripMargin
        )
        .assertBincompatSafe(scalaVersion)
    }

    test(s"Bincompat-friendly unions (Scala $scalaVersion)") {
      modelChanges(
        "baseline" ->
          s"""$modelPrefix
             |@bincompatFriendly
             |union Hello {
             |  s1: String
             |}
             |""".stripMargin,
        "memberAdded" ->
          s"""$modelPrefix
             |@bincompatFriendly
             |union Hello {
             |  s1: String
             |  s2: String
             |}
             |""".stripMargin
      )
        .withRunScalaCode(
          s"""|object Main extends App {
              |  val h = demo.Hello.s1("hello s1")
              |  println(h)
              |  assert(h.project.s1.get == "hello s1")
              |  h.accept(new demo.Hello.Visitor.Default[Unit] {
              |    def default: Unit = ()
              |
              |    override def s1(value: String): Unit = println(value)
              |  })
              |}
              |""".stripMargin
        )
        .assertBincompatSafe(scalaVersion)
    }

    // TODO
    test(s"Bincompat-friendly enums (Scala $scalaVersion)".ignore) {
      modelChanges(
        "baseline" ->
          s"""$modelPrefix
             |@bincompatFriendly
             |enum Hello {
             |  S1
             |}
             |""".stripMargin,
        "memberAdded" ->
          s"""$modelPrefix
             |@bincompatFriendly
             |enum Hello {
             |  S1
             |  S2
             |}
             |""".stripMargin
      ).assertBincompatSafe(scalaVersion)
    }

    test(s"Bincompat-friendly intEnums (Scala $scalaVersion)".ignore) {
      modelChanges(
        "baseline" ->
          s"""$modelPrefix
             |@bincompatFriendly
             |intEnum Hello {
             |  S1 = 1
             |}
             |""".stripMargin,
        "memberAdded" ->
          s"""$modelPrefix
             |@bincompatFriendly
             |intEnum Hello {
             |  S1 = 1
             |  S2 = 2
             |}
             |""".stripMargin
      ).assertBincompatSafe(scalaVersion)
    }
  }

  private def modelChanges(models: (String, String)*) =
    new ModelChangesBuilder(
      models = models,
      runScalaCode = None
    )

  case class ModelChangesBuilder(
      models: Seq[(String, String)],
      runScalaCode: Option[String]
  ) {

    def withRunScalaCode(code: String): ModelChangesBuilder =
      copy(runScalaCode = Some(code))

    def assertBincompatSafe(scalaVersion: String): Unit = {
      // These jars contain just the compiled generated code
      val jars = buildJars(
        models.toList.map((SmithyFile.apply _).tupled),
        scalaVersion = scalaVersion
      )

      // Check the jars against each other with MiMa
      checkMima(jars)

      runScalaCode.foreach { scalaCode =>
        // Compile generated code against each version, and run those with each future version
        // to ensure no linkage errors
        checkRuntime(
          jars = jars,
          scalaCode = scalaCode,
          scalaVersion = scalaVersion
        )
      }
    }

  }

  case class SmithyFile(modelName: String, text: String)

  private def buildJars(
      models: List[SmithyFile],
      scalaVersion: String
  ) = {
    models.map { smithyFile =>
      buildJar(
        modelName = smithyFile.modelName,
        smithyFiles = List(smithyFile),
        scalaVersion = scalaVersion
      )
    }
  }

  private def buildJar(
      modelName: String,
      smithyFiles: List[SmithyFile],
      scalaVersion: String,
      allowedNS: Option[Set[String]] = None
  ) = {
    val sources = generateCode(smithyFiles, allowedNS)

    val out = os.temp.dir() / s"model-$modelName-$scalaVersion.jar"
    scalaCli
      .packageJar(
        sourceDirectories = List(sources),
        outputJarPath = out,
        scalaVersion = scalaVersion,
        extraDeps = List(smithy4sCoreDependency)
      )
      .call(cwd = os.temp.dir())

    modelName -> out
  }

  // for each version, we want to compare all future versions against it, in the original order
  private def transitiveComparisons[A](as: List[A]): List[(A, List[A])] = {
    as.zipWithIndex
      .map { case (baseline, baselineIndex) =>
        val futureVersions = as.drop(baselineIndex + 1)
        (baseline, futureVersions)
      }
  }

  private def checkMima(
      jars: List[(String, os.Path)]
  ) = {
    transitiveComparisons(jars)
      .flatMap { case (baseline, futures) => futures.map(baseline -> _) }
      .foreach { case ((beforeName, beforeJar), (afterName, afterJar)) =>
        val problems = new MiMaLib(Nil)
          .collectProblems(
            oldJarOrDir = beforeJar.toIO,
            newJarOrDir = afterJar.toIO,
            excludeAnnots = Nil
          )
          .filter {
            // We only care about backwards compatibility problems
            case _: ReversedMissingMethodProblem => false
            case _                               => true
          }

        assert(
          clue(problems).isEmpty,
          s"Detected bincompat problems in the change: FROM `$beforeName` TO `$afterName`"
        )
      }
  }

  private def checkRuntime(
      jars: List[(String, os.Path)],
      scalaCode: String,
      scalaVersion: String
  ) = {
    transitiveComparisons(jars)
      .foreach { case (baseline, futures) =>
        val baselineJar = baseline._2

        val scalaCodePath = os.temp.dir() / "input.scala"
        os.write(scalaCodePath, scalaCode)

        val scalaJarPath = os.temp.dir() / "scala-output.jar"

        successOrElse(
          s"Failed to compile ${baseline._1} with Scala $scalaVersion and code:\n$scalaCode"
        ) {
          scalaCli
            .packageJar(
              scalaVersion = scalaVersion,
              outputJarPath = scalaJarPath,
              sourceDirectories = List(scalaCodePath),
              extraJars = List(baselineJar),
              extraDeps = List(smithy4sCoreDependency)
            )
            .call(cwd = os.temp.dir())
        }

        // We run with the version we compiled against, and all future versions afterwards
        (baseline +: futures).foreach { case (jarName, jar) =>
          successOrElse(
            s"Failed to run `$jarName` with Scala $scalaVersion against code compiled with `${baseline._1}`"
          ) {
            scalaCli
              .run(
                scalaVersion = scalaVersion,
                extraJars = List(jar, scalaJarPath),
                extraDeps = List(smithy4sCoreDependency)
              )
              .call(cwd = os.temp.dir())
          }
        }
      }
  }

  private def successOrElse[A](msg: String)(f: => A) =
    Either.catchNonFatal(f) match {
      case Right(v) => v
      case Left(e)  => fail(msg, e)
    }

  private def generateCode(
      smithyFiles: List[SmithyFile],
      allowedNS: Option[Set[String]]
  ): os.Path = {
    val inputDir = os.temp.dir()
    val outputDir = os.temp.dir()
    val scalaOutputDir = outputDir / "scala"

    val specFiles = smithyFiles.zipWithIndex.map { case (f, i) =>
      val specFile = inputDir / s"${f.modelName}-$i.smithy"
      os.write(specFile, f.text)
      specFile
    }

    Codegen.generateToDisk(
      CodegenArgs(
        specs = specFiles,
        output = scalaOutputDir,
        resourceOutput = outputDir / "resources",
        skip = Set(FileType.Resource),
        discoverModels = false,
        allowedNS = allowedNS,
        excludedNS = None,
        repositories = Nil,
        dependencies = Nil,
        transformers = Nil,
        localJars = Nil,
        smithyBuild = None
      )
    )

    scalaOutputDir
  }

  private object scalaCli {
    def packageJar(
        scalaVersion: String,
        outputJarPath: os.Path,
        sourceDirectories: List[os.Path],
        extraJars: List[os.Path] = Nil,
        extraDeps: List[String] = Nil
    ): os.proc = {
      os.proc(
        "scala-cli",
        "--power",
        "package",
        "--library",
        s"--scala=$scalaVersion",
        s"--output=$outputJarPath",
        extraJars.map { j => s"--jar=$j" },
        extraDeps.map { d => s"--dependency=$d" },
        sourceDirectories
      )
    }

    def run(
        scalaVersion: String,
        extraJars: List[os.Path] = Nil,
        extraDeps: List[String] = Nil
    ): os.proc =
      os.proc(
        "scala-cli",
        "--power",
        "run",
        s"--scala=$scalaVersion",
        extraJars.map { j => s"--jar=$j" },
        extraDeps.map { d => s"--dependency=$d" }
      )
  }

  private val smithy4sCoreDependency =
    // We're using a mutable version instead of BuildInfo
    // because we don't want a circular dependency - these tests support the codegen module, which core itself is generated with.
    s"${BuildInfo.smithy4sOrg}::smithy4s-core:latest.stable"

}
