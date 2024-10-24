/*
 *  Copyright 2021-2024 Disney Streaming
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

import smithy4s.codegen.{SmithyBuildJson, BuildInfo}
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.openapi.OpenApiVersion

import scala.collection.immutable.ListSet

final class SmithyBuildSpec extends munit.FunSuite {
  test("generate json with SmithyBuild.writeJson") {
    val actual = SmithyBuild.writeJson(
      SmithyBuild.Serializable(
        "1.0",
        ListSet("src/"),
        SmithyBuildMaven(
          ListSet("dep"),
          ListSet(SmithyBuildMavenRepository("repo"))
        )
      )
    )
    assertEquals(
      actual,
      """|{
         |    "version" : "1.0",
         |    "sources" : [
         |        "src/"
         |    ],
         |    "maven" : {
         |        "dependencies" : [
         |            "dep"
         |        ],
         |        "repositories" : [
         |            {
         |                "url" : "repo"
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
         |  "sources": ["src/main/smithy"],
         |  "maven": {
         |    "dependencies": ["oterh"],
         |    "repositories": []
         |  },
         |  "custom": "attribute"
         |}
         |""".stripMargin,
      """|{
         |  "version": "1.0",
         |  "sources": ["src/main/smithy"],
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
         |    "version" : "1.0",
         |    "sources" : [
         |        "src/main/smithy"
         |    ],
         |    "maven" : {
         |        "dependencies" : [
         |            "oterh",
         |            "dep1"
         |        ],
         |        "repositories" : [
         |        ]
         |    },
         |    "custom" : "attribute"
         |}""".stripMargin
    )
  }

  test("merge two json de-duplicating arrays") {
    val actual = SmithyBuildJson.merge(
      """|{
         |  "version": "1.0",
         |  "sources": ["src/main/smithy"],
         |  "maven": {
         |    "dependencies": ["oterh", "dep1"],
         |    "repositories": []
         |  }
         |}
         |""".stripMargin,
      """|{
         |  "version": "1.0",
         |  "sources": ["src/main/smithy"],
         |  "maven": {
         |    "dependencies": ["dep1"],
         |    "repositories": []
         |  },
         |  "custom": "attribute"
         |}
         |""".stripMargin
    )
    assertEquals(
      actual,
      """|{
         |    "version" : "1.0",
         |    "sources" : [
         |        "src/main/smithy"
         |    ],
         |    "maven" : {
         |        "dependencies" : [
         |            "oterh",
         |            "dep1"
         |        ],
         |        "repositories" : [
         |        ]
         |    },
         |    "custom" : "attribute"
         |}""".stripMargin
    )
  }

  test("Can parse smithy-build file with OpenAPI plugin") {
    val input =
      """
        |{
        |   "version": "1.0",
        |   "sources": [ "foo.smithy", "some/directory" ],
        |   "plugins": {
        |        "openapi": {
        |            "service": "example.weather#Weather",
        |            "version": "3.1.0"
        |        }
        |   }
        |}""".stripMargin

    val actual = io.circe.parser
      .decode[SmithyBuild](input)
      .left
      .map(x => throw x)
      .merge

    // OpenApiConfig doesn't override equals, so we need to check expected == actual in pieces:
    assertEquals(actual.maven, None)
    assertEquals("1.0", actual.version)
    assertEquals(
      Set[os.FilePath](
        os.FilePath("foo.smithy"),
        os.FilePath("some/directory")
      ),
      actual.sources.toSet
    )
    val actualOpenApiConfig = actual
      .getPlugin[SmithyBuildPlugin.OpenApi]
      .getOrElse(fail("No OpenAPI plugin on parsed smithy-build"))
      .config
    assertEquals(
      ShapeId.from("example.weather#Weather"),
      actualOpenApiConfig.getService.toShapeId
    )
    assertEquals(
      OpenApiVersion.VERSION_3_1_0,
      actualOpenApiConfig.getVersion
    )
  }

  test("Can parse smithy-build file with no optional fields") {
    val input =
      """
        |{
        |   "version": "1.0"
        |}""".stripMargin

    val actual = io.circe.parser
      .decode[SmithyBuild](input)
      .left
      .map(x => throw x)
      .merge

    assertEquals(actual.maven, None)
    assertEquals("1.0", actual.version)
    assertEquals(Set.empty[os.FilePath], actual.sources.toSet)
    assertEquals(Set.empty[SmithyBuildPlugin], actual.plugins.toSet)
  }

  test("generate json with SmithyBuildJson.toJson") {
    val actual = SmithyBuildJson.toJson(
      ListSet("src/"),
      ListSet("dep"),
      ListSet("repo")
    )
    assertEquals(
      actual,
      s"""|{
          |    "version" : "1.0",
          |    "sources" : [
          |        "src/"
          |    ],
          |    "maven" : {
          |        "dependencies" : [
          |            "dep",
          |            "com.disneystreaming.smithy4s:smithy4s-protocol:${BuildInfo.version}"
          |        ],
          |        "repositories" : [
          |            {
          |                "url" : "repo"
          |            }
          |        ]
          |    }
          |}""".stripMargin
    )
  }
}
