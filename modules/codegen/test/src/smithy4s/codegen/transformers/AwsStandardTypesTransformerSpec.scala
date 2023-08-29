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

package smithy4s.codegen.transformers

import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.{ModelSerializer, ShapeId}

final class AwsStandardTypesTransformerSpec extends munit.FunSuite {

  import smithy4s.codegen.internals.TestUtils._

  test(
    "Flattens member shapes targeting AWS new-types named after standard shapes"
  ) {
    val kinesisNamespace =
      """|$version: "2"
         |namespace com.amazonaws.kinesis
         |
         |// We expect this one to be removed in favour of smithy.api#Integer
         |integer Integer
         |// This one should be kept because it has a different name
         |timestamp Date
         |// This one should be kept because it has a trait
         |@range(min: 1)
         |long Long
         |""".stripMargin

    val testNamespace =
      """
        |namespace test
        |
        |structure TestStructure {
        | @required
        | i: com.amazonaws.kinesis#Integer
        | d: com.amazonaws.kinesis#Date
        | l: com.amazonaws.kinesis#Long,
        |}
        |""".stripMargin

    val rawModel = loadModel(kinesisNamespace, testNamespace)

    val transformer = new AwsStandardTypesTransformer()
    val transformerContext =
      TransformContext
        .builder()
        .model(rawModel)
        .build()

    val transformedModel = transformer.transform(transformerContext)

    val structureCode =
      generateScalaCode(transformedModel)("test.TestStructure")

    assertEquals(
      structureCode,
      """package test
        |
        |import com.amazonaws.kinesis.Date
        |import com.amazonaws.kinesis.Long
        |import smithy4s.Hints
        |import smithy4s.Schema
        |import smithy4s.ShapeId
        |import smithy4s.ShapeTag
        |import smithy4s.schema.Schema.int
        |import smithy4s.schema.Schema.struct
        |
        |case class TestStructure(i: Int, d: Option[Date] = None, l: Option[Long] = None)
        |
        |object TestStructure extends ShapeTag.Companion[TestStructure] {
        |  val id: ShapeId = ShapeId("test", "TestStructure")
        |
        |  val hints: Hints = Hints.empty
        |
        |  implicit val schema: Schema[TestStructure] = struct(
        |    int.required[TestStructure]("i", _.i).addHints(smithy.api.Required()),
        |    Date.schema.optional[TestStructure]("d", _.d),
        |    Long.schema.optional[TestStructure]("l", _.l),
        |  ){
        |    TestStructure.apply
        |  }.withId(id).addHints(hints)
        |}""".stripMargin
    )
  }

  test("Keeps member traits") {
    val kinesisNamespace =
      """|$version: "2"
         |
         |namespace com.amazonaws.kinesis
         |
         |string String
         |""".stripMargin

    val testNamespace =
      """
        |$version: "2"
        |
        |namespace test
        |
        |structure TestStructure {
        | @length(min:5, max:10)
        | s: com.amazonaws.kinesis#String
        |}
        |""".stripMargin

    val transformedModel =
      new AwsStandardTypesTransformer()
        .transform(
          TransformContext
            .builder()
            .model(loadModel(kinesisNamespace, testNamespace))
            .build()
        )

    val structureCode =
      generateScalaCode(transformedModel)("test.TestStructure")

    assertEquals(
      structureCode,
      """package test
        |
        |import smithy4s.Hints
        |import smithy4s.Schema
        |import smithy4s.ShapeId
        |import smithy4s.ShapeTag
        |import smithy4s.schema.Schema.string
        |import smithy4s.schema.Schema.struct
        |
        |case class TestStructure(s: Option[String] = None)
        |
        |object TestStructure extends ShapeTag.Companion[TestStructure] {
        |  val id: ShapeId = ShapeId("test", "TestStructure")
        |
        |  val hints: Hints = Hints.empty
        |
        |  implicit val schema: Schema[TestStructure] = struct(
        |    string.validated(smithy.api.Length(min = Some(5L), max = Some(10L))).optional[TestStructure]("s", _.s),
        |  ){
        |    TestStructure.apply
        |  }.withId(id).addHints(hints)
        |}""".stripMargin
    )
  }

  test("Keeps default traits") {
    val kinesisNamespace =
      """|$version: "2"
         |
         |namespace com.amazonaws.kinesis
         |
         |@default(5)
         |integer Integer
         |""".stripMargin

    val testNamespace =
      """
        |$version: "2"
        |
        |namespace test
        |
        |structure TestStructure {
        | i: com.amazonaws.kinesis#Integer
        |}
        |""".stripMargin

    val rawModel = loadModel(kinesisNamespace, testNamespace)

    val transformer = new AwsStandardTypesTransformer()
    val transformerContext =
      TransformContext
        .builder()
        .model(rawModel)
        .build()

    val transformedModel = transformer.transform(transformerContext)

    val structureCode =
      generateScalaCode(transformedModel)("test.TestStructure")

    assertEquals(
      structureCode,
      """package test
        |
        |import smithy4s.Hints
        |import smithy4s.Schema
        |import smithy4s.ShapeId
        |import smithy4s.ShapeTag
        |import smithy4s.schema.Schema.int
        |import smithy4s.schema.Schema.struct
        |
        |case class TestStructure(i: Int = 5)
        |
        |object TestStructure extends ShapeTag.Companion[TestStructure] {
        |  val id: ShapeId = ShapeId("test", "TestStructure")
        |
        |  val hints: Hints = Hints.empty
        |
        |  implicit val schema: Schema[TestStructure] = struct(
        |    int.required[TestStructure]("i", _.i).addHints(smithy.api.Default(smithy4s.Document.fromDouble(5.0d))),
        |  ){
        |    TestStructure.apply
        |  }.withId(id).addHints(hints)
        |}""".stripMargin
    )
  }

  test("Doesn't keep default traits on Lists") {
    val kinesisNamespace =
      """|$version: "2"
         |
         |namespace com.amazonaws.kinesis
         |
         |@default(5)
         |integer Integer
         |""".stripMargin

    val testNamespace =
      """
        |$version: "2"
        |
        |namespace test
        |
        |list TestList {
        |  member: com.amazonaws.kinesis#Integer
        |}
        |""".stripMargin

    val rawModel = loadModel(kinesisNamespace, testNamespace)

    val transformer = new AwsStandardTypesTransformer()
    val transformerContext =
      TransformContext
        .builder()
        .model(rawModel)
        .build()

    val transformedModel = transformer.transform(transformerContext)

    val listCode =
      generateScalaCode(transformedModel)("test.TestList")

    assertEquals(
      listCode,
      """package test
        |
        |import smithy4s.Hints
        |import smithy4s.Newtype
        |import smithy4s.Schema
        |import smithy4s.ShapeId
        |import smithy4s.schema.Schema.bijection
        |import smithy4s.schema.Schema.int
        |import smithy4s.schema.Schema.list
        |
        |object TestList extends Newtype[List[Int]] {
        |  val id: ShapeId = ShapeId("test", "TestList")
        |  val hints: Hints = Hints.empty
        |  val underlyingSchema: Schema[List[Int]] = list(int).withId(id).addHints(hints)
        |  implicit val schema: Schema[TestList] = bijection(underlyingSchema, asBijection)
        |}""".stripMargin
    )
  }

  test("Removes AWS new types after flattening") {
    val kinesisNamespace =
      """|namespace com.amazonaws.kinesis
         |
         |integer Integer
         |""".stripMargin

    val transformer = new AwsStandardTypesTransformer()
    val transformerContext =
      TransformContext
        .builder()
        .model(loadModel(kinesisNamespace))
        .build()

    val transformedModel = transformer.transform(transformerContext)

    assert(
      !transformedModel
        .getShape(ShapeId.from("com.amazonaws.kinesis#Integer"))
        .isPresent
    )
  }

  test("Flattens AWS dates") {
    val kinesisNamespace =
      """|namespace com.amazonaws.kinesis
         |
         |timestamp Date
         |timestamp Timestamp
         |""".stripMargin

    val transformer = new AwsStandardTypesTransformer()
    val transformerContext =
      TransformContext
        .builder()
        .model(loadModel(kinesisNamespace))
        .build()

    val transformedModel = transformer.transform(transformerContext)

    assert(
      transformedModel
        .getShape(ShapeId.from("com.amazonaws.kinesis#Date"))
        .isPresent
    )

    assert(
      !transformedModel
        .getShape(ShapeId.from("com.amazonaws.kinesis#Timestamp"))
        .isPresent
    )
  }

  test("Does not flatten types with traits") {
    val kinesisNamespace =
      """|namespace com.amazonaws.dynamodb
         |
         |@length(min: 1, max: 10)
         |string BackupArn
         |""".stripMargin

    val transformer = new AwsStandardTypesTransformer()
    val transformerContext =
      TransformContext
        .builder()
        .model(loadModel(kinesisNamespace))
        .build()

    val transformedModel = transformer.transform(transformerContext)

    assert(
      transformedModel
        .getShape(ShapeId.from("com.amazonaws.dynamodb#BackupArn"))
        .isPresent
    )
  }

  private def loadModel(namespaces: String*): Model = {
    val assembler = Model
      .assembler()
      .disableValidation()
      .discoverModels()

    namespaces
      .foldLeft(assembler) { case (a, model) =>
        a.addUnparsedModel(s"test-${model.hashCode}.smithy", model)
      }
      .assemble()
      .unwrap()
  }

  def prettyPrint(model: Model): String = {
    Node.prettyPrintJson(ModelSerializer.builder().build.serialize(model))
  }

}
