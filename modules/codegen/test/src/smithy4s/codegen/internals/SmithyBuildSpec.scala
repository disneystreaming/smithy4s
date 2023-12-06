/*
 *  Copyright 2021-2023 Disney Streaming
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

package smithy4s.codegen.internals

import smithy4s.codegen.SmithyBuildJson

final class SmithyBuildSpec extends munit.FunSuite {
  test("generate json") {
    val actual = SmithyBuild.writeJson(
      SmithyBuild(
        "1.0",
        List("src/"),
        SmithyBuildMaven(List("dep"), List(SmithyBuildMavenRepository("repo")))
      )
    )
    assertEquals(
      actual,
      """|{
         |    "version": "1.0",
         |    "imports": [
         |        "src/"
         |    ],
         |    "maven": {
         |        "dependencies": [
         |            "dep"
         |        ],
         |        "repositories": [
         |            {
         |                "url": "repo"
         |            }
         |        ]
         |    }
         |}""".stripMargin
    )
  }

  test("merge two json") {
    val actual = SmithyBuildJson.merge(
      """|{
         |  "version": "1.0",
         |  "imports": ["src/main/smithy"],
         |  "maven": {
         |    "dependencies": ["oterh"],
         |    "repositories": []
         |  },
         |  "custom": "attribute"
         |}
         |""".stripMargin,
      """|{
         |  "version": "1.0",
         |  "imports": ["src/main/smithy"],
         |  "maven": {
         |    "dependencies": ["dep1"],
         |    "repositories": []
         |  }
         |}
         |""".stripMargin
    )
    assertEquals(
      actual,
      """|{
         |    "version": "1.0",
         |    "imports": [
         |        "src/main/smithy"
         |    ],
         |    "maven": {
         |        "dependencies": [
         |            "oterh",
         |            "dep1"
         |        ],
         |        "repositories": []
         |    },
         |    "custom": "attribute"
         |}""".stripMargin
    )
  }

}
