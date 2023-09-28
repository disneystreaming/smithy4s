package smithy4s.codegen.mill

import mill._
import mill.scalalib._
import mill.testkit.MillTestKit
import sourcecode.FullName

class SmithyLSPConfigSpec extends munit.FunSuite {

  private object testKit extends MillTestKit

  test("config gets generated") {
    object root extends testKit.BaseModule {
      object foo extends Smithy4sModule {
        override def scalaVersion = "2.13.10"
        override def smithy4sAllowedNamespaces = T(Some(Set("aws.iam")))
        override def smithy4sIvyDeps = Agg(
          ivy"software.amazon.smithy:smithy-aws-iam-traits:${smithy4s.codegen.BuildInfo.smithyVersion}"
        )
      }
    }
    val ev =
      testKit.staticTestEvaluator(root)(FullName("config-gets-generated"))

    val result = ev(
      smithy4s.codegen.mill.Smithy4sExternalModule.generateSmithyBuildJson(
        ev.evaluator
      )
    ).map(_._1)

    assertEquals(
      result.isRight,
      true,
      s"Failed with the following error: ${result.swap.getOrElse("error unavailable")}"
    )

    val fileContent = os.read.stream(
      ev.evaluator.rootModule.millModuleBasePath.value / "smithy-build.json"
    )
    val writtenJson = ujson.read(fileContent)
    val expectedJson = ujson.read {
      s"""|{
          |  "version": "1.0",
          |  "imports": ["./foo/smithy"],
          |  "maven": {
          |    "dependencies": [
          |       "com.disneystreaming.alloy:alloy-core:${smithy4s.codegen.BuildInfo.alloyVersion}",
          |       "software.amazon.smithy:smithy-aws-iam-traits:${smithy4s.codegen.BuildInfo.smithyVersion}"
          |    ],
          |    "repositories": []
          |  }
          |}
          |""".stripMargin
    }
    assertEquals(writtenJson, expectedJson)
  }

}
