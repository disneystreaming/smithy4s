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

package smithy4s.codegen.internals

import software.amazon.smithy.model.{Model, SourceLocation}
import software.amazon.smithy.model.node.{
  Node,
  ArrayNode,
  ObjectNode,
  StringNode
}

import scala.jdk.CollectionConverters._

final class CodegenImplSpec extends munit.FunSuite {

  test("namespace clash") {
    val aSmithy =
      """namespace ns
        |
        |string Hello
        |
        |string GoodBye
        |""".stripMargin

    val bSmithy =
      """namespace ns
        |
        |// no hello
        |// string Hello
        |//
        |string GoodBye
        |""".stripMargin

    val objectNodeMap = Map[StringNode, Node](
      new StringNode(
        "namespaces",
        new SourceLocation("string node in test code")
      ) -> new ArrayNode(
        List[Node](
          new StringNode("ns", new SourceLocation("string node in test code"))
        ).asJava,
        new SourceLocation("objectNodeMap in test code")
      )
    ).asJava
    val repeatedNamespaceNode = new ObjectNode(
      objectNodeMap,
      new SourceLocation("repeatedNamespaceNode in test code")
    )
    val nodeList =
      List[Node](repeatedNamespaceNode, repeatedNamespaceNode).asJava
    val smithy4sMetadata = new ArrayNode(
      nodeList,
      new SourceLocation("smithy4sMetadata in test code")
    )

    val model = Model
      .assembler()
      .discoverModels()
      .addUnparsedModel("a.smithy", aSmithy)
      .addUnparsedModel("b.smithy", bSmithy)
      .putMetadata("smithy4sGenerated", smithy4sMetadata)
      .assemble()
      .unwrap()

    def generateScalaCode(model: Model): Map[String, String] = {
      CodegenImpl
        .generate(model, None, None)
        .map { case (_, result) =>
          s"${result.namespace}.${result.name}" -> result.content
        }
        .toMap
    }

    val expectedDuplicates = Seq(
      (
        "ns",
        Seq(
          new SourceLocation("repeatedNamespaceNode in test code"),
          new SourceLocation("repeatedNamespaceNode in test code")
        )
      )
    )

    val caught: RepeatedNamespaceException =
      intercept[RepeatedNamespaceException] {
        generateScalaCode(model)
      }
    assertEquals(caught.duplicates, expectedDuplicates)
  }
}
