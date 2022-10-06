/*
 *  Copyright 2021-2022 Disney Streaming
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

import mill.testkit.MillTestKit
import mill.scalalib._
import mill._
import munit.Location
import sourcecode.FullName
import java.nio.file.Paths

class Smithy4sModuleSpec extends munit.FunSuite {
  private val resourcePath =
    os.Path(Paths.get(this.getClass().getResource("/").toURI()))

  private object testKit extends MillTestKit

  private val coreDep =
    ivy"com.disneystreaming.smithy4s::smithy4s-core:${smithy4s.codegen.BuildInfo.version}"

  test("basic codegen runs") {
    object foo extends testKit.BaseModule with Smithy4sModule {
      override def scalaVersion = "2.13.8"
      override def ivyDeps = Agg(coreDep)
      override def millSourcePath = resourcePath / "basic"
    }
    val ev = testKit.staticTestEvaluator(foo)(FullName("codegen-runs"))

    compileWorks(foo, ev)
    checkFileExist(
      ev.outPath / "smithy4sOutputDir.dest" / "scala" / "basic" / "MyNewString.scala",
      shouldExist = true
    )
  }

  test("codegen with dependencies") {
    object foo extends testKit.BaseModule with Smithy4sModule {
      override def scalaVersion = "2.13.8"
      override def ivyDeps = Agg(coreDep)
      override def millSourcePath = resourcePath / "basic"
      override def smithy4sAllowedNamespaces = T(Some(Set("aws.iam")))
      override def smithy4sIvyDeps = Agg(
        ivy"software.amazon.smithy:smithy-aws-iam-traits:${smithy4s.codegen.BuildInfo.smithyVersion}"
      )
    }
    val ev =
      testKit.staticTestEvaluator(foo)(FullName("codegen-with-dependencies"))

    compileWorks(foo, ev)
    checkFileExist(
      ev.outPath / "smithy4sOutputDir.dest" / "scala" / "aws" / "iam" / "ActionPermissionDescription.scala",
      shouldExist = true
    )
  }

  test("multi-module codegen works") {

    object foo extends testKit.BaseModule with Smithy4sModule {
      override def scalaVersion = "2.13.8"
      override def ivyDeps = Agg(coreDep)
      override def millSourcePath = resourcePath / "multi-module" / "foo"
    }

    object bar extends testKit.BaseModule with Smithy4sModule {
      override def moduleDeps = Seq(foo)
      override def scalaVersion = "2.13.8"
      override def ivyDeps = Agg(coreDep)
      override def millSourcePath = resourcePath / "multi-module" / "bar"
    }

    val fooEv = testKit.staticTestEvaluator(foo)(FullName("multi-module-foo"))
    val barEv = testKit.staticTestEvaluator(bar)(FullName("multi-module-bar"))

    compileWorks(foo, fooEv)
    checkFileExist(
      fooEv.outPath / "smithy4sOutputDir.dest" / "scala" / "foo" / "Foo.scala",
      shouldExist = true
    )
    checkFileExist(
      fooEv.outPath / "smithy4sOutputDir.dest" / "scala" / "foodir" / "FooDir.scala",
      shouldExist = true
    )

    compileWorks(bar, barEv)
    checkFileExist(
      barEv.outPath / "smithy4sOutputDir.dest" / "scala" / "foo" / "Foo.scala",
      shouldExist = false
    )
    checkFileExist(
      barEv.outPath / "smithy4sOutputDir.dest" / "scala" / "foodir" / "FooDir.scala",
      shouldExist = false
    )
    checkFileExist(
      barEv.outPath / "smithy4sOutputDir.dest" / "scala" / "bar" / "Bar.scala",
      shouldExist = true
    )

    val aScalaPath = foo.millSourcePath / "src" / "a.scala"

    os.write(
      aScalaPath,
      """package foo
        |object a""".stripMargin,
      createFolders = true
    )

    try compileWorks(bar, barEv)
    finally {
      val _ =
        // cleaning up, because the target path doesn't get cleared automatically on test re-runs
        // (it's part of this test module's target path)
        os.remove.all(aScalaPath)
    }
  }

  private def compileWorks(
      sm: ScalaModule,
      testEvaluator: testKit.TestEvaluator
  )(implicit loc: Location) = {
    val result = testEvaluator(sm.compile).map(_._1)
    assertEquals(
      result.isRight,
      true,
      s"Failed with the following error: ${result.swap.getOrElse("error unavailable")}"
    )
  }

  private def checkFileExist(path: os.Path, shouldExist: Boolean) = {
    if (!os.exists(path) && shouldExist) {
      sys.error(s"${path} file not found")
    }
    if (os.exists(path) && !shouldExist) {
      sys.error(s"${path} file should not exist")
    }
  }
}
