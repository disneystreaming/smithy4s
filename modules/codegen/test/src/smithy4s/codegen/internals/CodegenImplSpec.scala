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

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.SourceLocation
import software.amazon.smithy.model.node.ArrayNode
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.node.StringNode

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

  test("applies allowed namespace filter correctly") {
    namespaceFilterTest(
      inputNamespaces = List(
        "allowed.namespace",
        "allowed.namespace.nested",
        "not.allowed.namespace"
      ),
      allowedNamespaces = List("allowed.namespace")
    )(expectedCodegenNamespaces = Set("allowed.namespace"))
  }

  test("applies allowed namespace filter correctly (with wildcard)") {
    namespaceFilterTest(
      inputNamespaces = List(
        "allowed.namespace",
        "allowed.namespace.nested",
        "not.allowed.namespace"
      ),
      allowedNamespaces = List("allowed.namespace*")
    )(expectedCodegenNamespaces =
      Set("allowed.namespace", "allowed.namespace.nested")
    )
  }

  test("applies forbidden namespace filter correctly") {
    namespaceFilterTest(
      inputNamespaces = List(
        "allowed.namespace",
        "allowed.namespace.nested",
        "forbidden.namespace"
      ),
      forbiddenNamespaces = List("forbidden.namespace")
    )(expectedCodegenNamespaces =
      Set("allowed.namespace", "allowed.namespace.nested")
    )
  }

  test("applies disallowed namespace filter correctly") {
    namespaceFilterTest(
      inputNamespaces = List(
        "forbidden.namespace",
        "forbidden.namespace.nested",
        "allowed.namespace"
      ),
      forbiddenNamespaces = List("forbidden.namespace*")
    )(expectedCodegenNamespaces = Set("allowed.namespace"))
  }

  test(
    "applies allowed and disallowed correctly - must be both allowed and not excluded"
  ) {
    namespaceFilterTest(
      inputNamespaces = List(
        "allowed.namespace",
        "allowed.namespace.nested",
        "allowed.namespace.forbidden.too",
        "not.allowed.namespace",
        "forbidden.namespace"
      ),
      allowedNamespaces = List("allowed.namespace*"),
      forbiddenNamespaces =
        List("forbidden.namespace", "allowed.namespace.forbidden.too")
    )(expectedCodegenNamespaces =
      Set("allowed.namespace", "allowed.namespace.nested")
    )
  }

  private def namespaceFilterTest(
      inputNamespaces: List[String],
      allowedNamespaces: List[String] = Nil,
      forbiddenNamespaces: List[String] = Nil
  )(expectedCodegenNamespaces: Set[String]) = {
    def mkSpec(namespace: String) = {
      s"""|
          |namespace $namespace
          |
          |string Dummy
      """.stripMargin
    }
    def mkModel(specs: List[String]): Model = {
      specs.zipWithIndex
        .foldLeft(Model.assembler().discoverModels()) {
          case (builder, (spec, index)) =>
            builder.addUnparsedModel(s"spec-$index.smithy", spec)
        }
        .assemble()
        .unwrap()
    }
    val model = mkModel(inputNamespaces.map(mkSpec))
    val generatedNamespaces = CodegenImpl
      .generate(
        model,
        Option(allowedNamespaces.toSet).filter(_.nonEmpty),
        Option(forbiddenNamespaces.toSet).filter(_.nonEmpty)
      )
      .map { case (_, result) =>
        result.namespace
      }
      .toSet
    assertEquals(generatedNamespaces, expectedCodegenNamespaces)

  }
}
