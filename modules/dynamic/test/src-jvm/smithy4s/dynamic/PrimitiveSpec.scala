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

package smithy4s.dynamic

import munit.Location
import smithy4s.Schema.PrimitiveSchema
import smithy4s.ShapeId
import smithy4s.schema.Primitive
import software.amazon.smithy.model.{Model => SModel}

class PrimitiveSpec extends DummyIO.Suite {
  val smithy = """
    $version: "2"
    namespace example

    @alloy#uuidFormat
    string MyUuid

    timestamp MyTimestamp

    @alloy#dateFormat
    string MyLocalDate

    @alloy#localTimeFormat
    string MyLocalTime

    @alloy#durationSecondsFormat
    bigDecimal MyDuration

    @alloy#offsetDateTimeFormat
    @timestampFormat("date-time")
    timestamp MyOffsetDateTime
  """

  val dynamicSchema = {
    val model =
      SModel
        .assembler()
        .addUnparsedModel("dynamic.smithy", smithy)
        .discoverModels(this.getClass().getClassLoader())
        .assemble()
        .unwrap()

    DynamicSchemaIndex.loadModel(model)
  }

  def assertPrimitive[A](
      shapeId: ShapeId,
      expectedPrimitive: Primitive[A]
  )(implicit
      loc: Location
  ) = {
    val schema = dynamicSchema
      .getSchema(shapeId)
      .getOrElse(fail("Error: shape missing"))

    val primitive = schema match {
      case PrimitiveSchema(_, _, tag) => tag.asInstanceOf[Primitive[A]]
      case unexpected                 => fail(s"Unexpected schema: $unexpected")
    }

    assertEquals(primitive, expectedPrimitive)
  }

  test("dynamic schema supports UUID") {
    assertPrimitive(
      ShapeId("example", "MyUuid"),
      Primitive.PUUID
    )
  }

  test("dynamic schema support Timestamp") {
    assertPrimitive(
      ShapeId("example", "MyTimestamp"),
      Primitive.PTimestamp
    )
  }

  test("dynamic schema support LocalDate") {
    assertPrimitive(
      ShapeId("example", "MyLocalDate"),
      Primitive.PLocalDate
    )
  }
  test("dynamic schema support LocalTime") {
    assertPrimitive(
      ShapeId("example", "MyLocalTime"),
      Primitive.PLocalTime
    )
  }
  test("dynamic schema support Duration") {
    assertPrimitive(
      ShapeId("example", "MyDuration"),
      Primitive.PDuration
    )
  }
  test("dynamic schema support OffsetDateTime") {
    assertPrimitive(
      ShapeId("example", "MyOffsetDateTime"),
      Primitive.POffsetDateTime
    )
  }

}
