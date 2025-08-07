package smithy4s.codegen

import munit.FunSuite
import com.typesafe.tools.mima.lib.MiMaLib
import cats.syntax.all._
import com.typesafe.tools.mima.core.ReversedMissingMethodProblem

class BincompatCodegenIntegrationSpec extends FunSuite {
  private val scala212 = "2.12"
  private val scala213 = "2.13"
  private val scala3 = "3"
  private val scalaVersions = List(scala212, scala213, scala3)

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
             |  @alloy#nullable @bincompatAdded(version: "2.0.0") s4: String
             |  s9: String
             |}
             |""".stripMargin
      )
        .withRunScalaCode(
          s"""|object Main extends App {
              |  println(demo.Hello("hello s1"))
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
        .pipe {
          // 2.12 has exhaustivity checking problems, so we don't check for this at all
          if (scalaVersion != scala212)
            _.withExpectedCompilationError(
              s"""|//> using option -Xfatal-warnings
                  |object Main extends App {
                  |  val h: demo.Hello = demo.Hello.S1Case("hello s1")
                  |  println(h)
                  |  assert(h.project.s1.get == "hello s1")
                  |  h match {
                  |    case demo.Hello.S1Case(value) => println(value)
                  |  }
                  |}
                  |""".stripMargin,
              "match may not be exhaustive"
            )
          else identity

        }
        .assertBincompatSafe(scalaVersion)
    }

    test(s"Bincompat-friendly enums (Scala $scalaVersion)") {
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
      )
        // with this code, we check for the "Unreachable case" warning
        // which should NOT appear if the enum is properly bincompat-friendly
        .withRunScalaCode(
          s"""|//> using option -Xfatal-warnings
              |object Main extends App {
              |  val h = demo.Hello.S1
              |  println(h)
              |  assert(h.stringValue == "S1")
              |  demo.Hello.values.foreach {
              |    case demo.Hello.S1 => println("S1 value")
              |    case _ => println("Unknown value")
              |  }
              |}
              |""".stripMargin
        )
        .withExpectedCompilationError(
          s"""|//> using option -Xfatal-warnings
              |object Main extends App {
              |  demo.Hello.values.foreach {
              |    case demo.Hello.S1 => println("S1 value")
              |  }
              |}
              |""".stripMargin,
          "match may not be exhaustive"
        )
        .assertBincompatSafe(scalaVersion)
    }

    test(s"Bincompat-friendly intEnums (Scala $scalaVersion)") {
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

    // This test takes tens of seconds on each Scala version
    // and it's unlikely a particular version matters here
    // so to save time in development we just use one version
    if (scalaVersion == scala213)
      test(s"Bincompat-friendly struct trait (Scala $scalaVersion)") {
        val traitModels = List(
          "V1_baseline" ->
            s"""$modelPrefix
               |@bincompatFriendly
               |@trait
               |structure HelloTrait {
               |  @required s1: String
               |  s9: String
               |}
               |""".stripMargin,
          "V2_withAdditions" ->
            s"""$modelPrefix
               |@bincompatFriendly
               |@trait
               |structure HelloTrait {
               |  @required s1: String = "s1"
               |  @bincompatAdded(version: "1.0.0") s2: String
               |  @bincompatAdded(version: "2.0.0") @required s3: String = "s3Default"
               |  s9: String
               |}
               |""".stripMargin,
          "V3_moreAdditions" ->
            s"""$modelPrefix
               |@bincompatFriendly
               |@trait
               |structure HelloTrait {
               |  @required s1: String = "s1"
               |  @bincompatAdded(version: "1.0.0") s2: String
               |  @bincompatAdded(version: "2.0.0") @required s3: String = "s3Default"
               |  s9: String
               |  @bincompatAdded(version: "3.0.0") s4: String
               |}
               |""".stripMargin
        ).map((SmithyFile.apply _).tupled)

        val traitUsageNamespace = "trait_usage"
        val traitUsageModels = List(
          "V1_baseline" ->
            s"""$$version: "2"
               |namespace $traitUsageNamespace
               |
               |@demo#HelloTrait(s1: "hello s1", s9: "hello s9")
               |structure MyStruct {}
               |""".stripMargin,
          "V2_withAdditions" ->
            s"""$$version: "2"
               |namespace $traitUsageNamespace
               |
               |@demo#HelloTrait(s1: "hello s1", s2: "hello s2", s3: "hello s3", s9: "hello s9")
               |structure MyStruct {}
               |""".stripMargin,
          "V3_moreAdditions" ->
            s"""$$version: "2"
               |namespace $traitUsageNamespace
               |
               |@demo#HelloTrait(s1: "hello s1", s2: "hello s2", s3: "hello s3", s9: "hello s9")
               |structure MyStruct {}
               |""".stripMargin
        ).map((SmithyFile.apply _).tupled)

        val scalaCode = s"""|object Main extends App {
                            |  val h = trait_usage.MyStruct()
                            |  println(h)
                            |  trait_usage.MyStruct.hints.all.foreach(println)
                            |}
                            |""".stripMargin

        val stats = traitUsageTest(
          traitModels = traitModels,
          traitUsageModels = traitUsageModels,
          scalaCode = scalaCode,
          scalaVersion = scalaVersion,
          traitUsageNamespace = traitUsageNamespace
        )

        assertEquals(
          stats,
          TraitUsageTestStats(
            usageJarCount = 6,
            mainJarCount = 10,
            // With 2 files it's 11 runs, with 4 files it's 203
            runCount = 57
          )
        )
      }
  }

  private case class TraitUsageTestStats(
      usageJarCount: Int,
      mainJarCount: Int,
      runCount: Int
  )

  /**
     * Tests all legal combinations (backward compatibility, i.e. you can run against things on a more recent version that you compiled against)
     * of generated trait code, and generated trait usage code.
     */
  private def traitUsageTest(
      traitModels: List[SmithyFile],
      traitUsageModels: List[SmithyFile],
      scalaCode: String,
      scalaVersion: String,
      traitUsageNamespace: String
  ): TraitUsageTestStats = {
    case class TraitJar(
        file: os.Path,
        version: String
    )
    case class TraitUsageJar(
        file: os.Path,
        version: String,
        traitJarVersion: String
    )
    case class MainJar(
        file: os.Path,
        usageJar: TraitUsageJar,
        traitJarVersion: String
    )

    val traitJars: List[TraitJar] = buildJars(
      traitModels,
      scalaVersion = scalaVersion
    ).map(
      { case (modelName, jar) =>
        TraitJar(jar, modelName)
      }
    )

    val traitUsageJars =
      traitUsageModels
        .flatMap { traitUsageSmithyFile =>
          // We need an exact match for the trait model
          val correspondingTraitModel =
            traitModels
              .find(_.modelName == traitUsageSmithyFile.modelName)
              .getOrElse(
                fail(
                  s"Could not find corresponding trait model for ${traitUsageSmithyFile.modelName}"
                )
              )

          // But the compile-time jars have to be AT LEAST as new as the trait model
          val compatibleTraitJars = traitJars
            .filter(_.version >= traitUsageSmithyFile.modelName)

          compatibleTraitJars.map { traitJar =>
            val (_, traitUsageJar) = buildJar(
              modelName = "trait-usage-" + traitUsageSmithyFile.modelName,
              smithyFiles = List(traitUsageSmithyFile, correspondingTraitModel),
              allowedNS = Some(Set(traitUsageNamespace)),
              scalaVersion = scalaVersion,
              extraJars = List(traitJar.file)
            )

            TraitUsageJar(
              file = traitUsageJar,
              version = traitUsageSmithyFile.modelName,
              traitJarVersion = traitJar.version
            )
          }
        }

    val mainJars = for {
      traitJar <- traitJars

      traitUsageJar <- traitUsageJars
      if traitJar.version >= traitUsageJar.traitJarVersion
    } yield {

      val outFile = os.temp.dir() / "out.jar"

      successOrElse(
        s"failed to build main jar. Trait jar: $traitJar, traitUsageJar: $traitUsageJar"
      ) {
        val scalaFile = os.temp.dir() / "input.scala"
        os.write(scalaFile, scalaCode)

        scalaCli
          .packageJar(
            scalaVersion = scalaVersion,
            outputJarPath = outFile,
            sourceDirectories = List(scalaFile),
            extraJars = List(traitJar.file, traitUsageJar.file),
            extraDeps = List(smithy4sCoreDependency)
          )
          .call(cwd = os.temp.dir())

        MainJar(
          file = outFile,
          usageJar = traitUsageJar,
          traitJarVersion = traitJar.version
        )
      }
    }

    val runs = for {
      traitJar <- traitJars

      traitUsageJar <- traitUsageJars
      if traitJar.version >= traitUsageJar.traitJarVersion

      mainJar <- mainJars
      if traitJar.version >= mainJar.traitJarVersion
      if traitJar.version >= mainJar.usageJar.traitJarVersion
      if traitUsageJar.version >= mainJar.usageJar.version
    } yield cats.Eval.later {
      successOrElse(
        s"failed to run Scala code. Main jar: $mainJar, traitUsageJar: $traitUsageJar, traitJar: $traitJar"
      ) {
        scalaCli
          .run(
            scalaVersion = scalaVersion,
            extraJars = List(traitJar.file, traitUsageJar.file, mainJar.file),
            extraDeps = List(smithy4sCoreDependency)
          )
          .call(cwd = os.temp.dir())
      }
    }

    println(s"Performing ${runs.size} runs for Scala $scalaVersion...")
    runs.foreach(_.value)
    println("Finished runs for Scala " + scalaVersion)

    TraitUsageTestStats(
      usageJarCount = traitUsageJars.size,
      mainJarCount = mainJars.size,
      runCount = runs.size
    )
  }

  private def modelChanges(models: (String, String)*) =
    new ModelChangesBuilder(
      models = models,
      runScalaCode = None,
      notCompilingScalaCode = None
    )

  case class NotCompilingScalaCode(code: String, expectedError: String)

  case class ModelChangesBuilder(
      models: Seq[(String, String)],
      runScalaCode: Option[String],
      notCompilingScalaCode: Option[NotCompilingScalaCode]
  ) {

    def withRunScalaCode(code: String): ModelChangesBuilder =
      copy(runScalaCode = Some(code))

    def withExpectedCompilationError(
        code: String,
        expectedError: String
    ): ModelChangesBuilder = {
      copy(
        notCompilingScalaCode = Some(NotCompilingScalaCode(code, expectedError))
      )
    }

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

      notCompilingScalaCode.foreach { code =>
        jars.foreach { case (modelName, jar) =>
          val sourceFile = os.temp.dir() / s"$modelName.scala"
          os.write(sourceFile, code.code)
          val pkgd = scalaCli
            .packageJar(
              scalaVersion = scalaVersion,
              outputJarPath = os.temp.dir() / "output.jar",
              sourceDirectories = List(sourceFile),
              extraJars = List(jar),
              extraDeps = List(smithy4sCoreDependency)
            )
            .spawn(cwd = os.temp.dir(), stderr = os.Pipe)

          val stderrText = sanitizeConsole(pkgd.stderr.text())

          pkgd.waitFor()

          assert(
            stderrText.contains("Error compiling project"),
            s"""Expected compilation to fail, but it didn't (model $modelName).
               |Stderr:
               |$stderrText""".stripMargin
          )

          assert(
            stderrText.contains(code.expectedError),
            s"""Expected to find the following error in the compilation output (model $modelName):
               |${code.expectedError}
               |But got:
               |$stderrText""".stripMargin
          )
        }
      }
    }

  }

  // sanitizes text by removing ANSI escape codes
  private def sanitizeConsole(s: String): String = {
    "\u001B\\[[;\\d]*[a-zA-Z]".r.replaceAllIn(s, "")
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
      allowedNS: Option[Set[String]] = None,
      extraJars: List[os.Path] = Nil
  ) = {
    val sources = generateCode(smithyFiles, allowedNS)

    val out = os.temp.dir() / s"model-$modelName-$scalaVersion.jar"
    scalaCli
      .packageJar(
        sourceDirectories = List(sources),
        outputJarPath = out,
        scalaVersion = scalaVersion,
        extraDeps = List(smithy4sCoreDependency),
        extraJars = extraJars
      )
      // 60000 == 60 seconds
      .call(cwd = os.temp.dir(), stderr = os.Pipe, timeout = 60000)

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
            // Unions' visitor traits are immune to reversed missing method problems
            // because the interface is sealed. If there's a new union member (new method in the visitor),
            // the default visitor is guaranteed to have that method.
            case prob: ReversedMissingMethodProblem
                if prob.meth.owner.fullName.endsWith("$Visitor") =>
              false
            case _ => true
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
          val pkgd = scalaCli
            .packageJar(
              scalaVersion = scalaVersion,
              outputJarPath = scalaJarPath,
              sourceDirectories = List(scalaCodePath),
              extraJars = List(baselineJar),
              extraDeps = List(smithy4sCoreDependency)
            )
            .spawn(cwd = os.temp.dir(), stderr = os.Pipe)

          // Workaround for https://github.com/VirtusLab/scala-cli/issues/3735 - the exit code isn't non-zero when fatal warnings are present
          val stderrText = pkgd.stderr.text()

          pkgd.waitFor()

          assert(
            !stderrText.contains("Error compiling project"),
            s"Found compilation errors: $stderrText"
          )
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
        dependencies = List(
          s"${BuildInfo.alloyOrg}:alloy-core:${BuildInfo.alloyVersion}"
        ),
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
        "--repository",
        "ivy2local",
        "--library",
        s"--scala=$scalaVersion",
        s"--output=$outputJarPath",
        extraJars.map { j => s"--compile-only-jar=$j" },
        extraDeps.map { d => s"--compile-only-dependency=$d" },
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
    s"${BuildInfo.smithy4sOrg}::smithy4s-core:${BuildInfo.version}"

// polyfill for Scala 2.12
  private implicit class PipeOps[A](private val self: A) {
    def pipe[B](f: A => B): B =
      f(self)
  }
}
