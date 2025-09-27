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

package smithy4s.codegen.transformers

import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ShapeId

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import cats.syntax.all._

final class NamespacesPrefixerSpec extends munit.FunSuite {

  private val prefixer = new NamespacesPrefixer()

  private val defaultMetadata = Map(
    "smithy4sTransformedNamespacesPrefix" -> Node.from(
      "com.example.transformed"
    ),
    "smithy4sTransformedNamespacesNamespacesToTransform" -> Node.arrayNode(
      Node.from("mynamespace")
    )
  )

  test("NamespacesPrefixer skips transformation when no metadata is provided") {
    val smithyContent = """
                          |$version: "2"
                          |namespace mynamespace
                          |
                          |structure TestStructure {
                          |    field: String
                          |}
    """.stripMargin

    val model =
      buildModelFromSmithy(smithyContent = smithyContent, metadata = Map.empty)
    val context = createTransformContext(model)

    val transformedModel = prefixer.transform(context)

    assertEquals(transformedModel.shapes().count(), model.shapes().count())
    assert(
      transformedModel
        .getShape(ShapeId.from("mynamespace#TestStructure"))
        .isPresent
    )
  }

  test("NamespacesPrefixer transforms namespace when metadata is provided") {
    val smithyContent = """
                          |$version: "2"
                          |namespace mynamespace
                          |
                          |structure TestStructure {
                          |    field: String
                          |}
    """.stripMargin

    val model = buildModelFromSmithy(smithyContent, defaultMetadata)
    val context = createTransformContext(model)
    val transformedModel = prefixer.transform(context)

    assert(
      transformedModel
        .getShape(
          ShapeId.from("com.example.transformed.mynamespace#TestStructure")
        )
        .isPresent
    )
    assert(
      !transformedModel
        .getShape(ShapeId.from("mynamespace#TestStructure"))
        .isPresent
    )
  }

  test(
    "NamespacesPrefixer fails when namespacesToTransform is defined but prefix is not"
  ) {
    val smithyContent = """
                          |$version: "2"
                          |namespace mynamespace
                          |
                          |structure TestStructure {
                          |    field: String
                          |}
    """.stripMargin

    val metadata = Map(
      "smithy4sTransformedNamespacesNamespacesToTransform" -> Node.arrayNode(
        Node.from("mynamespace")
      )
    )

    val model = buildModelFromSmithy(smithyContent, metadata)
    val result = NamespacesPrefixer.constructParams(model)
    assertEquals(
      result.left.toOption,
      "smithy4sTransformedNamespacesNamespacesToTransform is defined with length >= 1 but smithy4sTransformedNamespacesPrefix is not defined".some
    )
  }

  test(
    "NamespacesPrefixer fails when prefix is defined but namespacesToTransform is not"
  ) {
    val smithyContent = """
                          |$version: "2"
                          |namespace mynamespace
                          |
                          |structure TestStructure {
                          |    field: String
                          |}
    """.stripMargin

    val metadata = Map(
      "smithy4sTransformedNamespacesPrefix" -> Node.from(
        "com.example.transformed"
      )
    )

    val model = buildModelFromSmithy(smithyContent, metadata)
    val result = NamespacesPrefixer.constructParams(model)
    assertEquals(
      result.left.toOption,
      "smithy4sTransformedNamespacesPrefix is defined but smithy4sTransformedNamespacesNamespacesToTransform is not defined or has length 0".some
    )
  }

  test("NamespacesPrefixer fails when namespacesToTransform is empty array") {
    val smithyContent = """
                          |$version: "2"
                          |namespace mynamespace
                          |
                          |structure TestStructure {
                          |    field: String
                          |}
    """.stripMargin

    val metadata = Map(
      "smithy4sTransformedNamespacesPrefix" -> Node.from(
        "com.example.transformed"
      ),
      "smithy4sTransformedNamespacesNamespacesToTransform" -> Node.arrayNode()
    )

    val model = buildModelFromSmithy(smithyContent, metadata)
    val result = NamespacesPrefixer.constructParams(model)
    assertEquals(
      result.left.toOption,
      "smithy4sTransformedNamespacesPrefix is defined but smithy4sTransformedNamespacesNamespacesToTransform is not defined or has length 0".some
    )
  }

  test("NamespacesPrefixer transforms only matching namespaces") {
    val namespacedContent = """
                              |$version: "2"
                              |namespace mynamespace
                              |
                              |structure NamespacedStructure {
                              |    field: String
                              |}
    """.stripMargin

    val otherContent = """
                         |$version: "2"
                         |namespace other
                         |
                         |structure OtherStructure {
                         |    field: String
                         |}
    """.stripMargin

    val model = buildModelFromSmithy(
      smithyContent = List(
        ("mynamespace.smithy", namespacedContent),
        ("other.smithy", otherContent)
      ),
      metadata = defaultMetadata
    )
    val context = createTransformContext(model)
    val transformedModel = prefixer.transform(context)

    assert(
      transformedModel
        .getShape(
          ShapeId.from(
            "com.example.transformed.mynamespace#NamespacedStructure"
          )
        )
        .isPresent
    )
    assert(
      transformedModel.getShape(ShapeId.from("other#OtherStructure")).isPresent
    )
    assert(
      !transformedModel
        .getShape(ShapeId.from("mynamespace#NamespacedStructure"))
        .isPresent
    )
    assert(
      !transformedModel
        .getShape(ShapeId.from("com.example.transformed.other#OtherStructure"))
        .isPresent
    )
  }

  test("NamespacesPrefixer transforms structures and its members") {
    val smithyContent = """
                          |$version: "2"
                          |namespace mynamespace
                          |
                          |structure InnerStructure {
                          |    innerField: String
                          |}
                          |
                          |structure OuterStructure {
                          |    field: InnerStructure
                          |    anotherField: String
                          |}
    """.stripMargin

    val model = buildModelFromSmithy(smithyContent, defaultMetadata)
    val context = createTransformContext(model)
    val transformedModel = prefixer.transform(context)

    val outerShape = transformedModel
      .getShape(
        ShapeId.from("com.example.transformed.mynamespace#OuterStructure")
      )
      .get()
    val outerMembers = outerShape.getAllMembers.asScala
    assertEquals(
      outerMembers("field").getTarget.toString,
      "com.example.transformed.mynamespace#InnerStructure"
    )
    assertEquals(
      outerMembers("anotherField").getTarget.toString,
      "smithy.api#String"
    )

    val innerShape = transformedModel
      .getShape(
        ShapeId.from("com.example.transformed.mynamespace#InnerStructure")
      )
      .get()
    val innerMembers = innerShape.getAllMembers.asScala
    assertEquals(
      innerMembers("innerField").getTarget.toString,
      "smithy.api#String"
    )
  }

  test("NamespacesPrefixer transforms unions and its members") {
    val smithyContent = """
                          |$version: "2"
                          |namespace mynamespace
                          |
                          |union TestUnion {
                          |    stringValue: String
                          |    bar: ContainerStructure
                          |}
                          |
                          |structure ContainerStructure {
                          |    foo: String
                          |}
    """.stripMargin

    val model = buildModelFromSmithy(smithyContent, defaultMetadata)
    val context = createTransformContext(model)
    val transformedModel = prefixer.transform(context)

    val unionShape = transformedModel
      .getShape(ShapeId.from("com.example.transformed.mynamespace#TestUnion"))
      .get()
    val unionMembers = unionShape.getAllMembers.asScala
    assertEquals(
      unionMembers("stringValue").getTarget.toString,
      "smithy.api#String"
    )
    assertEquals(
      unionMembers("bar").getTarget.toString,
      "com.example.transformed.mynamespace#ContainerStructure"
    )

    val containerShape = transformedModel
      .getShape(
        ShapeId.from("com.example.transformed.mynamespace#ContainerStructure")
      )
      .get()
    val containerMembers = containerShape.getAllMembers.asScala
    assertEquals(
      containerMembers("foo").getTarget.toString,
      "smithy.api#String"
    )
  }

  test("NamespacesPrefixer transforms mixins for a structure") {
    val smithyContent = """
                          |$version: "2"
                          |namespace mynamespace
                          |
                          |@mixin
                          |structure MixinStructure {
                          |    foo: String
                          |}
                          |
                          |structure MainStructure with [MixinStructure] {
                          |    bar: String
                          |}
    """.stripMargin

    val model = buildModelFromSmithy(smithyContent, defaultMetadata)
    val context = createTransformContext(model)
    val transformedModel = prefixer.transform(context)

    val mainShape = transformedModel
      .getShape(
        ShapeId.from("com.example.transformed.mynamespace#MainStructure")
      )
      .get()
    val mainMembers = mainShape.getAllMembers.asScala
    assertEquals(mainMembers("foo").getTarget.toString, "smithy.api#String")
    assertEquals(mainMembers("bar").getTarget.toString, "smithy.api#String")

    val mixinShape = transformedModel
      .getShape(
        ShapeId.from("com.example.transformed.mynamespace#MixinStructure")
      )
      .get()
    val mixinMembers = mixinShape.getAllMembers.asScala
    assertEquals(mixinMembers("foo").getTarget.toString, "smithy.api#String")

    val mixinIds = mainShape.getMixins.asScala.map(_.toString).toSet
    assertEquals(
      mixinIds,
      Set("com.example.transformed.mynamespace#MixinStructure")
    )
  }

  test("NamespacesPrefixer transforms traits for a shape") {
    val smithyContent = """
                          |$version: "2"
                          |namespace mynamespace
                          |
                          |@trait
                          |structure MyTrait {
                          |    foo: String
                          |}
                          |
                          |@MyTrait(foo: "bar")
                          |structure MainStructure {
                          |    bar: String
                          |}
    """.stripMargin

    val model = buildModelFromSmithy(smithyContent, defaultMetadata)
    val context = createTransformContext(model)
    val transformedModel = prefixer.transform(context)

    val traitIds = getTraitsFor(
      transformedModel,
      "com.example.transformed.mynamespace#MainStructure"
    )

    assert(
      traitIds.contains(
        ShapeId.from("com.example.transformed.mynamespace#MyTrait")
      )
    )
  }

  test("NamespacesPrefixer transforms traits for a member shape") {
    val smithyContent = """
                          |$version: "2"
                          |namespace mynamespace
                          |
                          |@trait
                          |structure MyTrait {
                          |    foo: String
                          |}
                          |
                          |structure MainStructure {
                          |    @MyTrait(foo: "bar")
                          |    bar: String
                          |}
    """.stripMargin

    val model = buildModelFromSmithy(smithyContent, defaultMetadata)
    val context = createTransformContext(model)
    val transformedModel = prefixer.transform(context)

    val traitIds = getTraitsFor(
      transformedModel,
      "com.example.transformed.mynamespace#MainStructure$bar"
    )

    assert(
      traitIds.contains(
        ShapeId.from("com.example.transformed.mynamespace#MyTrait")
      )
    )
  }

  test("NamespacesPrefixer transforms mixins for a string shape") {
    val smithyContent = """
                          |$version: "2"
                          |namespace mynamespace
                          |
                          |@mixin
                          |@pattern("[a-zA-Z0-1]*")
                          |string AlphaNumericMixin
                          |
                          |@length(min: 8, max: 32)
                          |string Username with [AlphaNumericMixin]
    """.stripMargin

    val model = buildModelFromSmithy(smithyContent, defaultMetadata)
    val context = createTransformContext(model)
    val transformedModel = prefixer.transform(context)

    val usernameShape = transformedModel
      .getShape(
        ShapeId.from("com.example.transformed.mynamespace#Username")
      )
      .get()
    val mixins = usernameShape.getMixins.asScala

    assert(
      mixins.contains(
        ShapeId.from("com.example.transformed.mynamespace#AlphaNumericMixin")
      )
    )
  }

  private def getTraitsFor(model: Model, shapeId: String): Set[ShapeId] =
    model
      .getShape(ShapeId.from(shapeId))
      .toScala
      .map {
        _.getAllTraits().keySet().asScala.toSet
      }
      .getOrElse(fail(s"Expected to find shape with id $shapeId"))

  private def buildModelFromSmithy(
      smithyContent: String,
      metadata: Map[String, Node]
  ): Model = buildModelFromSmithy(
    smithyContent = List(("test.smithy", smithyContent)),
    metadata = metadata
  )

  private def buildModelFromSmithy(
      smithyContent: List[(String, String)],
      metadata: Map[String, Node]
  ): Model = {
    val assembler = smithyContent.foldLeft(
      Model
        .assembler()
    ) { case (assembler, (filename, content)) =>
      assembler.addUnparsedModel(filename, content)
    }

    metadata.foreach { case (key, value) =>
      assembler.putMetadata(key, value)
    }

    assembler.assemble().unwrap()
  }

  private def createTransformContext(model: Model): TransformContext = {
    TransformContext
      .builder()
      .model(model)
      .projectionName("test")
      .build()
  }

}
