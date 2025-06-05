/*
 *  Copyright 2021-2025 Disney Streaming
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

import _root_.mill._
import _root_.mill.scalalib._
import _root_.mill.testkit._
import java.nio.file.Paths

class SmithyLSPConfigSpec extends munit.FunSuite {

  private val resourcePath =
    os.Path(Paths.get(this.getClass().getResource("/").toURI()))

  test("config gets generated") {
    object root extends TestBaseModule {
      object foo extends Smithy4sModule {
        override def repositoriesTask = T.task {
          super.repositoriesTask() ++
            Seq(
              coursier.MavenRepository(
                "https://some.corpo.example.com/artifactory"
              )
            )
        }
        override def scalaVersion = "2.13.16"
        override def smithy4sAllowedNamespaces = T(Some(Set("aws.iam")))
        override def smithy4sIvyDeps = Agg(
          ivy"software.amazon.smithy:smithy-aws-iam-traits:${smithy4s.codegen.BuildInfo.smithyVersion}"
        )
      }
    }

    UnitTester(root, resourcePath).scoped { eval =>
      val result = eval("smithy4s.codegen.LSP/updateConfig")
      assertEquals(
        result.isRight,
        true,
        s"Failed with the following error: ${result.swap.getOrElse("error unavailable")}"
      )

      val configPath = eval.outPath / os.up / "smithy-build.json"
      val fileContent = os.read.stream(configPath)
      val writtenJson = ujson.read(fileContent)

      val expectedJson = ujson.read {
        s"""|{
            |  "version": "1.0",
            |  "sources": ["./foo/smithy"],
            |  "maven": {
            |    "dependencies": [
            |       "com.disneystreaming.alloy:alloy-core:${smithy4s.codegen.BuildInfo.alloyVersion}",
            |       "software.amazon.smithy:smithy-aws-iam-traits:${smithy4s.codegen.BuildInfo.smithyVersion}",
            |       "com.disneystreaming.smithy4s:smithy4s-protocol:${smithy4s.codegen.BuildInfo.version}"
            |    ],
            |    "repositories": [
            |       { "url": "https://some.corpo.example.com/artifactory" }
            |    ]
            |  }
            |}
            |""".stripMargin
      }

      assertEquals(writtenJson, expectedJson)
    }
  }
}
