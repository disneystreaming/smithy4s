package smithy4s.codegen.mill

import mill.testkit.MillTestKit
import mill.scalalib._
import mill._
import munit.Location
import sourcecode.FullName

class Smithy4sModuleSpec extends munit.FunSuite {
  object testKit extends MillTestKit

  object foo extends testKit.BaseModule with Smithy4sModule {
    override def scalaVersion = "2.13.8"
    override def ivyDeps = Agg(
      ivy"com.disneystreaming.smithy4s::smithy4s-core:0.15.3"
    )
  }

  object bar extends testKit.BaseModule with Smithy4sModule {
    override def moduleDeps = Seq(foo)
    override def scalaVersion = "2.13.8"
    override def ivyDeps = Agg(
      ivy"com.disneystreaming.smithy4s::smithy4s-core:0.15.3"
    )
  }

  test("codegen runs") {
    val ev = testKit.staticTestEvaluator(foo)(FullName("codegen-runs"))
    os.write.over(
      foo.millSourcePath / "smithy" / "foo.smithy",
      s"""|$$version: "2"
          |
          |namespace basic
          |
          |string MyNewString""".stripMargin,
      createFolders = true
    )

    compileWorks(foo, ev)
    checkFileExist(
      ev.outPath / "smithy4sOutputDir.dest" / "scala" / "basic" / "MyNewString.scala",
      shouldExist = true
    )
  }

  test("multi-module codegen works") {
    val fooEv = testKit.staticTestEvaluator(foo)(FullName("multi-module-foo"))
    val barEv = testKit.staticTestEvaluator(foo)(FullName("multi-module-bar"))
    os.write.over(
      foo.millSourcePath / "smithy" / "foo.smithy",
      s"""|$$version: "2.0"
          |
          |namespace foo
          |
          |structure Foo {
          |  a: Integer
          |}""".stripMargin,
      createFolders = true
    )

    os.write.over(
      bar.millSourcePath / "smithy" / "bar.smithy",
      s"""|$$version: "2.0"
          |
          |namespace bar
          |
          |use foo#Foo
          |
          |// Checking that Foo can be found by virtue of the bar project depending on the foo project
          |structure Bar {
          |  foo: Foo
          |}""".stripMargin,
      createFolders = true
    )

    os.write.over(
      bar.millSourcePath / "src" / "Test.scala",
      s"""|package bar
          |
          |import foo._
          |
          |object BarTest {
          |
          |  def main(args: Array[String]): Unit = println(Bar(Some(Foo(Some(1)))))
          |
          |}""".stripMargin,
      createFolders = true
    )

    compileWorks(foo, fooEv)
    checkFileExist(
      fooEv.outPath / "smithy4sOutputDir.dest" / "scala" / "foo" / "Foo.scala",
      shouldExist = true
    )

    compileWorks(bar, barEv)
    checkFileExist(
      barEv.outPath / "smithy4sOutputDir.dest" / "scala" / "foo" / "Foo.scala",
      shouldExist = false
    )
    checkFileExist(
      barEv.outPath / "smithy4sOutputDir.dest" / "scala" / "bar" / "Bar.scala",
      shouldExist = true
    )
  }

  private def compileWorks(
      sm: ScalaModule,
      testEvaluator: testKit.TestEvaluator
  )(implicit loc: Location) = {
    val result = testEvaluator(sm.compile).map(_._1)
    assertEquals(result.isRight, true)
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
