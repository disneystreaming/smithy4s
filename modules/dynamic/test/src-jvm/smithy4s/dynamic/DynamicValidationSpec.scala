/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s.dynamic

import smithy4s.Document
import smithy4s.ShapeId
import smithy4s.schema.Schema
import software.amazon.smithy.model.Model

class DynamicValidationSpec extends DummyIO.Suite {

  test("shape constraints are enforced") {
    val spec = """
      $version: "2"
      namespace example

      structure Foo {
          bar: ShortString
      }

      @length(min: 1, max: 3)
      string ShortString
    """

    indexTest(
      spec,
      ShapeId("example", "Foo"),
      Document.obj(
        "bar" -> Document.fromString("foobar")
      ),
      "length required to be >= 1 and <= 3, but was 6"
    )

  }

  test("struct-field inline constraint is enforced") {
    val smithy = """
      $version: "2"
      namespace example

      structure Foo {
          @length(min: 1, max: 3)
          bar: String
      }
    """

    indexTest(
      smithy,
      ShapeId("example", "Foo"),
      Document.obj("bar" -> Document.fromString("foobar")),
      "length required to be >= 1 and <= 3, but was 6"
    )
  }

  test("list member constraint is enforced") {
    val smithy = """
      $version: "2"
      namespace example

      structure Foo {
          tags: Tags
      }

      list Tags {
          @length(min: 1, max: 3)
          member: String
      }
    """

    indexTest(
      smithy,
      ShapeId("example", "Foo"),
      Document.obj("tags" -> Document.array(Document.fromString("foobar"))),
      "length required to be >= 1 and <= 3, but was 6"
    )
  }

  test("map key/value constraints are enforced") {
    val smithy = """
      $version: "2"
      namespace example

      structure Foo {
          extra: Extra
      }

      map Extra {
          @length(min: 2)
          key: String
          @length(min: 2, max: 10)
          value: String
      }
    """

    indexTest(
      smithy,
      ShapeId("example", "Foo"),
      Document.obj(
        "extra" -> Document.obj("ab" -> Document.fromString("foobarbazqux"))
      ),
      "length required to be >= 2 and <= 10, but was 12"
    )
  }

  test("enum constraint is enforced") {
    val smithy = """
      $version: "2"
      namespace example

      structure Foo {
          @length(min: 2)
          letter: Letters
      }

      enum Letters {
          A
          B
          C
      }
    """

    indexTest(
      smithy,
      ShapeId("example", "Foo"),
      Document.obj("letter" -> Document.fromString("A")),
      "length required to be >= 2, but was 1"
    )
  }

  test(
    "a member-level constraint layered on top of a shape-level constraint are both independently enforced"
  ) {
    val smithy = """
      $version: "2"
      namespace example

      @length(min: 1, max: 3)
      string ShortString

      structure Foo {
          tags: Tags
      }

      list Tags {
          @pattern("^[a-z]+$")
          member: ShortString
      }
    """
    val shapeId = ShapeId("example", "Foo")
    def taggedWith(value: String) =
      Document.obj("tags" -> Document.array(Document.fromString(value)))

    // violates the shape-level @length
    indexTest(
      smithy,
      shapeId,
      taggedWith("abcdef"),
      "length required to be >= 1 and <= 3, but was 6"
    )
    // violates the member-level @pattern
    indexTest(
      smithy,
      shapeId,
      taggedWith("AB"),
      "String 'AB' does not match pattern '^[a-z]+$'"
    )

    // satisfies both the shape-level @length and the member-level @pattern,
    // regardless of applySchemaRefinements
    val valid = decodeAgainstShape(
      smithy,
      shapeId,
      taggedWith("abc"),
      applySchemaRefinements = true
    )
    assert(valid.isRight, s"Expected decoding to succeed, got: $valid")
  }

  test("operation input constraints are enforced") {
    val smithy = """
      $version: "2"
      namespace example

      service Foo {
          operations: [Op]
      }

      operation Op {
          input: OpInput
      }

      structure OpInput {
          @length(min: 1, max: 3)
          bar: String
      }
    """

    indexOperationTest(
      smithy,
      ShapeId("example", "Foo"),
      Document.obj("bar" -> Document.fromString("foobar")),
      "length required to be >= 1 and <= 3, but was 6"
    )
  }

  private def assertFailsWith(
      result: Either[smithy4s.codecs.PayloadError, _],
      expectedMessageContains: String
  ) = result match {
    case Left(error) =>
      assert(
        error.getMessage.contains(expectedMessageContains),
        s"Expected error message to contain '$expectedMessageContains', got: ${error.getMessage}"
      )
    case Right(_) =>
      fail(s"Expected decoding to fail, got: $result")
  }

  private def indexTest(
      smithy: String,
      shapeId: ShapeId,
      document: Document,
      expectedMessageContains: String
  ) = {
    val turnedOn = decodeAgainstShape(
      smithy,
      shapeId,
      document,
      applySchemaRefinements = true
    )
    assertFailsWith(turnedOn, expectedMessageContains)
    val turnedOff = decodeAgainstShape(
      smithy,
      shapeId,
      document,
      applySchemaRefinements = false
    )
    assert(
      turnedOff.isRight,
      s"Expected decoding to succeed, got: $turnedOff"
    )
  }

  private def indexOperationTest(
      smithy: String,
      serviceId: ShapeId,
      document: Document,
      expectedMessageContains: String
  ) = {
    val turnedOn = decodeAgainstOperationInput(
      smithy,
      serviceId,
      document,
      applySchemaRefinements = true
    )
    assertFailsWith(turnedOn, expectedMessageContains)
    val turnedOff = decodeAgainstOperationInput(
      smithy,
      serviceId,
      document,
      applySchemaRefinements = false
    )
    assert(
      turnedOff.isRight,
      s"Expected decoding to succeed, got: $turnedOff"
    )
  }

  private def parseSmithy(smithy: String): Model =
    Model
      .assembler()
      .addUnparsedModel("dynamic.smithy", smithy)
      .discoverModels(this.getClass().getClassLoader())
      .assemble()
      .unwrap()

  private def loadIndex(
      smithy: String,
      applySchemaRefinements: Boolean
  ): DynamicSchemaIndex =
    DynamicSchemaIndex.loadModel(
      parseSmithy(smithy),
      applySchemaRefinements = applySchemaRefinements
    )

  private def decodeAgainstSchema(
      schema: Schema[_],
      document: Document
  ) = Document.Decoder.fromSchema(schema).decode(document)

  private def decodeAgainstShape(
      smithy: String,
      shapeId: ShapeId,
      document: Document,
      applySchemaRefinements: Boolean
  ) = {
    val index = loadIndex(smithy, applySchemaRefinements)
    val schema = index
      .getSchema(shapeId)
      .getOrElse(fail("Error: shape missing"))
    decodeAgainstSchema(schema, document)
  }

  private def decodeAgainstOperationInput(
      smithy: String,
      serviceId: ShapeId,
      document: Document,
      applySchemaRefinements: Boolean
  ) = {
    val index = loadIndex(smithy, applySchemaRefinements)
    val service = index
      .getService(serviceId)
      .getOrElse(fail("Error: service missing"))
    val inputSchema = service.service.endpoints.head.input
    decodeAgainstSchema(inputSchema, document)
  }

}
