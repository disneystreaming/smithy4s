/*
 *  Copyright 2021-2026 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.codegen.mill

import coursier.Repository
import coursier.ivy.IvyRepository
import mill._
import mill.api.Discover
import mill.scalalib._
import mill.scalalib.publish.PomSettings
import mill.scalalib.publish.VersionControl
import mill.testkit.TestRootModule
import mill.testkit.UnitTester
import munit.Location

import java.nio.file.Paths

import scala.concurrent.duration._

class Smithy4sModuleSpec extends munit.FunSuite {
  private val resourcePath =
    os.Path(Paths.get(this.getClass().getResource("/").toURI()))

  override val munitTimeout = 5.minutes

  private val coreDep =
    mvn"com.disneystreaming.smithy4s::smithy4s-core:${smithy4s.codegen.BuildInfo.version}"

  test("basic codegen runs") {
    object foo extends TestRootModule with Smithy4sModule {
      lazy val millDiscover = Discover[this.type]
      override def scalaVersion = "2.13.18"
      override def mvnDeps = Seq(coreDep)
    }

    val resourceFolder = resourcePath / "basic"
    UnitTester(foo, resourceFolder).scoped { eval =>
      // Evaluating tasks by direct reference
      val result = eval(foo.compile)
      assertEquals(
        result.isRight,
        true,
        s"Failed with the following error: ${result.swap.getOrElse("error unavailable")}"
      )

      eval(foo.smithy4sOutputDir) match {
        case Right(outputDir) =>
          checkFileExist(
            outputDir.value.path / "basic" / "MyNewString.scala",
            shouldExist = true
          )
          withFile(
            foo.moduleDir / "smithy" / "added.smithy",
            """namespace basic
              |
              |structure Added {}""".stripMargin
          )(eval(foo.compile))

          checkFileExist(
            outputDir.value.path / "basic" / "Added.scala",
            shouldExist = true
          )

        case _ => fail("aaa")
      }

    }

  }

  test("wildcard settings") {
    class Test(version: String, options: Seq[String])
        extends TestRootModule
        with Smithy4sModule {
      lazy val millDiscover = Discover[this.type]
      override def scalaVersion = version
      override def scalacOptions = options
    }

    def getArg(version: String, options: Seq[String]): String = {
      val module = new Test(version, options)
      val resourceFolder = resourcePath / "wildcard"
      UnitTester(module, resourceFolder).scoped { eval =>
        val result = eval(module.smithy4sWildcardArgument)
        assertEquals(
          result.isRight,
          true,
          s"Failed with the following error: ${result.swap.getOrElse("error unavailable")}"
        )
        result.toOption.get.value.head.toString
      }
    }

    val msg1 = """use "_" if major version is not 3"""
    assertEquals(getArg("2.13.18", Seq()), "_", msg1)
    assertEquals(getArg("2.13.18", Seq("-source", "future")), "_", msg1)
    assertEquals(getArg("2.13.18", Seq("-source:future")), "_", msg1)

    val msg2 =
      """use "?" if major version >= 3.1 or using -source:future or -source future"""
    assertEquals(getArg("3.1.foobar", Seq()), "?", msg2)
    assertEquals(getArg("3.0.foobar", Seq("-source", "future")), "?", msg2)
    assertEquals(getArg("3.0.foobar", Seq("-source:future")), "?", msg2)

    val msg3 =
      """use "_" if major version < 3.1 and not using -source:future or -source future"""
    assertEquals(getArg("3.0.foobar", Seq()), "_", msg3)
    assertEquals(getArg("3.foobar.foobar", Seq()), "_", msg3)
  }

  test("codegen with wildcards") {
    object foo extends TestRootModule with Smithy4sModule {
      lazy val millDiscover = Discover[this.type]
      override def scalaVersion = "3.3.0"
      override def mvnDeps = Seq(coreDep)
      override def scalacOptions = Seq("-Xfatal-warnings", "-source", "future")
    }

    val resourceFolder = resourcePath / "service"
    UnitTester(foo, resourceFolder).scoped { eval =>
      val compileResult = eval(foo.compile)
      println(compileResult)
      assertEquals(
        compileResult.isRight,
        true,
        s"Compilation failed: ${compileResult.swap.getOrElse("unknown error")}"
      )

      val metadataFile =
        eval(foo.smithy4sGeneratedSmithyMetadataFile).toOption.get.value.path

      checkFileExist(metadataFile, shouldExist = true)

      assert(
        os.read(metadataFile)
          .contains("metadata smithy4sWildcardArgument = \"?\""),
        clue = "Expected metadata to contain wildcard assignment"
      )
    }
  }

  test("2.13 codegen with placeholder wildcards") {
    object foo extends TestRootModule with Smithy4sModule {
      lazy val millDiscover = Discover[this.type]
      override def scalaVersion = "2.13.18"
      override def mvnDeps = Seq(coreDep)
      override def scalacPluginMvnDeps =
        Seq(mvn"org.typelevel:::kind-projector:0.13.4")
      override def scalacOptions =
        Seq("-Xsource:3", "-P:kind-projector:underscore-placeholders")

    }

    val resourceFolder = resourcePath / "service"
    UnitTester(foo, resourceFolder).scoped { eval =>
      val compileResult = eval(foo.compile)
      assertEquals(
        compileResult.isRight,
        true,
        s"Compilation failed: ${compileResult.swap.getOrElse("unknown error")}"
      )

      val metadataFile =
        eval(foo.smithy4sGeneratedSmithyMetadataFile).toOption.get.value.path

      checkFileExist(metadataFile, shouldExist = true)

      assert(
        os.read(metadataFile)
          .contains("metadata smithy4sWildcardArgument = \"?\""),
        clue = "Expected metadata to contain wildcard assignment"
      )
    }
  }

  test("codegen with dependencies") {
    object foo extends TestRootModule with Smithy4sModule {
      lazy val millDiscover = Discover[this.type]
      override def scalaVersion = "2.13.18"
      override def mvnDeps = Seq(coreDep)
      override def smithy4sAllowedNamespaces: T[Option[Set[String]]] =
        Task(Some(Set("aws.iam")))
      override def smithy4sIvyDeps = Task {
        Seq(
          mvn"software.amazon.smithy:smithy-aws-iam-traits:${smithy4s.codegen.BuildInfo.smithyVersion}"
        )
      }
    }

    val resourceFolder = resourcePath / "basic"
    UnitTester(foo, resourceFolder).scoped { eval =>
      val compileResult = eval(foo.compile)
      assertEquals(
        compileResult.isRight,
        true,
        s"Compilation failed: ${compileResult.swap.getOrElse("unknown error")}"
      )

      val outputDir = eval(foo.smithy4sOutputDir).toOption.get.value.path
      val filePath =
        outputDir / "aws" / "iam" / "ActionPermissionDescription.scala"

      checkFileExist(filePath, shouldExist = true)
    }
  }

  test("codegen with custom smithy-build.json works") {
    object foo extends TestRootModule with Smithy4sModule {
      lazy val millDiscover = Discover[this.type]
      override def scalaVersion = "2.13.18"
      override def mvnDeps = Seq(coreDep)

      override def smithyBuild: T[Option[PathRef]] = Task.Input {
        Some(PathRef(moduleDir / "smithy-build.json"))
      }
    }

    val resourceFolder = resourcePath / "smithy-build"
    UnitTester(foo, resourceFolder).scoped { eval =>
      val compileResult = eval(foo.compile)
      assertEquals(
        compileResult.isRight,
        true,
        s"Compilation failed: ${compileResult.swap.getOrElse("unknown error")}"
      )

      val openApiFile =
        eval(
          foo.smithy4sResourceOutputDir
        ).toOption.get.value.path / "smithy4s.example.ObjectService.json"

      checkFileExist(openApiFile, shouldExist = true)

      val openApiJson = os.read(openApiFile)
      assert(
        openApiJson.contains("X-Bar"),
        "Smithy Build openApi configuration was not applied"
      )
    }
  }
  test("multi-module codegen works") {
    object base extends TestRootModule {
      lazy val millDiscover = Discover[this.type]
      object foo extends Smithy4sModule {
        override def scalaVersion = "2.13.18"
        override def mvnDeps = Seq(coreDep)
      }

      object bar extends Smithy4sModule {
        override def scalaVersion = "2.13.18"
        override def mvnDeps = Seq(coreDep)
        override def moduleDeps: Seq[JavaModule] = Seq(foo)
      }
    }

    val baseResource = resourcePath / "multi-module"
    UnitTester(base, baseResource).scoped { eval =>
      val fooCompile = eval(base.foo.compile)
      assertEquals(
        fooCompile.isRight,
        true,
        s"Foo compile failed: ${fooCompile.swap.getOrElse("unknown error")}"
      )

      val fooOutput = eval(base.foo.smithy4sOutputDir).toOption.get.value.path

      checkFileExist(fooOutput / "foo" / "Foo.scala", shouldExist = true)
      checkFileExist(fooOutput / "foodir" / "FooDir.scala", shouldExist = true)

      val barCompile = eval(base.bar.compile)
      assertEquals(
        barCompile.isRight,
        true,
        s"Bar compile failed: ${barCompile.swap.getOrElse("unknown error")}"
      )

      val barOutput = eval(base.bar.smithy4sOutputDir).toOption.get.value.path

      checkFileExist(barOutput / "foo" / "Foo.scala", shouldExist = false)
      checkFileExist(
        barOutput / "foodir" / "FooDir.scala",
        shouldExist = false
      )
      checkFileExist(barOutput / "bar" / "Bar.scala", shouldExist = true)

      withFile(
        base.foo.moduleDir / "src" / "a.scala",
        """package foo
          |object a""".stripMargin
      )(eval(base.bar.compile))
    }
  }

  test("multi-module codegen works with AWS specs upstream") {
    object base extends TestRootModule {
      lazy val millDiscover = Discover[this.type]
      object foo extends Smithy4sModule {
        override def scalaVersion = "2.13.18"
        override def mvnDeps = Seq(
          mvn"com.disneystreaming.smithy4s::smithy4s-aws-kernel:${smithy4s.codegen.BuildInfo.version}"
        )
        override def smithy4sIvyDeps: T[Seq[Dep]] = Task {
          Seq(
            mvn"software.amazon.smithy:smithy-aws-traits:${smithy4s.codegen.BuildInfo.smithyVersion}"
          )
        }
      }

      object bar extends Smithy4sModule {
        override def scalaVersion = "2.13.18"
        override def moduleDeps: Seq[JavaModule] = Seq(foo)
      }
    }

    val baseResource = resourcePath / "multi-module-aws"
    UnitTester(base, baseResource).scoped { eval =>
      val fooCompile = eval(base.foo.compile)
      assertEquals(
        fooCompile.isRight,
        true,
        s"foo compile failed: ${fooCompile.swap.getOrElse("unknown error")}"
      )

      val fooOutput = eval(base.foo.smithy4sOutputDir).toOption.get.value.path
      checkFileExist(fooOutput / "foo" / "Lambda.scala", shouldExist = true)
      checkFileExist(fooOutput / "aws", shouldExist = false)

      val barCompile = eval(base.bar.compile)
      assertEquals(
        barCompile.isRight,
        true,
        s"bar compile failed: ${barCompile.swap.getOrElse("unknown error")}"
      )

      val barOutput = eval(base.bar.smithy4sOutputDir).toOption.get.value.path
      checkFileExist(barOutput / "foo" / "Lambda.scala", shouldExist = false)
    }
  }

  private def withFile[A](path: os.Path, content: String)(f: => A): A = {
    os.write(path, content, createFolders = true)
    try f
    finally
    // we need to clean up, because we copy files to the target path
    // (which doesn't get cleared automatically on test re-runs)
    os.remove.all(path)
  }

  test(
    "multi-module codegen doesn't trigger upstream compilation when opted out"
  ) {
    object base extends TestRootModule {
      lazy val millDiscover = Discover[this.type]
      object foo extends ScalaModule {
        override def scalaVersion = "2.13.18"
      }

      object bar extends Smithy4sModule {
        override def scalaVersion = "2.13.18"
        override def moduleDeps: Seq[JavaModule] = Seq(foo)
        override def mvnDeps = Seq(coreDep)

        override def smithy4sInternalDependenciesAsJars = Task {
          List.empty[PathRef]
        }
      }
    }

    val baseResource = resourcePath / "multi-module-no-compile"

    UnitTester(base, baseResource).scoped { eval =>
      val result = eval(base.bar.smithy4sCodegen)
      assertEquals(
        result.isRight,
        true,
        s"smithy4sCodegen failed: ${result.swap.getOrElse("unknown error")}"
      )
    }
  }

  test("multi-module staged codegen works") {
    val localIvyRepo = os.temp.dir() / ".ivy2" / "local"

    trait Common extends SbtModule with Smithy4sModule with PublishModule {
      override def scalaVersion = "2.13.18"
      override def repositoriesTask: mill.api.Task[Seq[Repository]] =
        Task.Anon {
          val ivy2Local = IvyRepository.fromPattern(
            (localIvyRepo.toNIO.toUri.toString + "/") +: coursier.ivy.Pattern.default,
            dropInfoAttributes = true
          )
          Seq(ivy2Local) ++ super.repositoriesTask()
        }
      def pomSettings: T[PomSettings] = PomSettings(
        "foo",
        "foobar",
        "http://foobar",
        Seq.empty,
        VersionControl(),
        Seq.empty
      )
      def publishVersion: T[String] = "0.0.1-SNAPSHOT"

    }

    object base extends TestRootModule {
      lazy val millDiscover = Discover[this.type]
      object foo extends Common {
        override def artifactName: T[String] = "foo-mill"
        override def scalaVersion = "2.13.18"
        override def mvnDeps = Seq(coreDep)
        override def smithy4sAllowedNamespaces: T[Option[Set[String]]] =
          Some(Set("aws.api", "foo"))
        override def moduleDir =
          resourcePath / "multimodule-staged" / "foo"
        // foo refers to smithy-aws-traits explicitly as a code-gen only dep, and upon publishing,
        // this information is stored in the manifest of bar's jar, for downstream consumption
        override def smithy4sIvyDeps = Task {
          Seq(
            mvn"software.amazon.smithy:smithy-aws-traits:${smithy4s.codegen.BuildInfo.smithyVersion}"
          )
        }
      }

      object bar extends Common {
        override def artifactName: T[String] = "bar-mill"
        override def scalaVersion = "2.13.18"
        // bar depend on foo as a library, and an assumption is made that bar may depend on the same smithy models
        // that foo depended on for its own codegen. Therefore, these are retrieved from foo's manifest,
        // resolved and added to the list of jars to seek smithy models from during code generation
        override def mvnDeps = Task {
          super.mvnDeps() ++ Seq(
            mvn"${pomSettings().organization}::foo-mill:${publishVersion()}"
          )
        }
        override def moduleDir =
          resourcePath / "multimodule-staged" / "bar"
      }
    }

    val baseResource = resourcePath / "multimodule-staged"
    UnitTester(base, baseResource).scoped { eval =>
      val publishResult = eval(base.foo.publishLocal(localIvyRepo.toString()))
      assertEquals(
        publishResult.isRight,
        true,
        s"publishLocal failed: ${publishResult.swap.getOrElse("unknown error")}"
      )
      val compileResult = eval(base.bar.compile)
      assertEquals(
        compileResult.isRight,
        true,
        s"compile failed: ${compileResult.swap.getOrElse("unknown error")}"
      )

      val barOutput = eval(base.bar.smithy4sOutputDir).toOption.get.value.path
      checkFileExist(
        barOutput / "bar" / "Bar.scala",
        shouldExist = true
      )
      checkFileExist(
        barOutput / "foo" / "Foo.scala",
        shouldExist = false
      )
    }

  }

  test("codegen with aws specs") {
    object foo extends TestRootModule with Smithy4sModule {
      lazy val millDiscover = Discover[this.type]
      override def scalaVersion = "2.13.18"
      override def mvnDeps = Seq(coreDep)
      override def smithy4sAwsSpecs: T[Seq[String]] = Task(Seq(AWS.dynamodb))
    }

    UnitTester(foo, resourcePath).scoped { eval =>
      val result = eval(foo.smithy4sCodegen)
      assertEquals(
        result.isRight,
        true,
        s"Codegen failed: ${result.swap.getOrElse("unknown error")}"
      )

      val output = eval(foo.smithy4sOutputDir).toOption.get.value.path

      val file =
        output / "com" / "amazonaws" / "dynamodb" / "AttributeValue.scala"
      checkFileExist(file, shouldExist = true)
    }
  }

  private def checkFileExist(path: os.Path, shouldExist: Boolean)(implicit
      loc: Location
  ) = {
    if (!os.exists(path) && shouldExist) {
      fail(s"${path} file not found")
    }
    if (os.exists(path) && !shouldExist) {
      fail(s"${path} file should not exist")
    }
  }
}
