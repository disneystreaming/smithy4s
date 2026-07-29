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
import software.amazon.smithy.model.Model

class DynamicValidationSpec extends DummyIO.Suite {

  test("loadModel does not enforce constraint traits by default") {
    val decoded = decodeInvalidDocument(applySchemaRefinements = false)
    assert(decoded.isRight, s"Expected decoding to succeed, got: $decoded")
  }

  test("loadModel(applySchemaRefinements = true) enforces constraint traits") {
    val decoded = decodeInvalidDocument(applySchemaRefinements = true)
    assert(decoded.isLeft, s"Expected decoding to fail, got: $decoded")
  }

  private def decodeInvalidDocument(applySchemaRefinements: Boolean) = {

    val smithy = """
      $version: "2"
      namespace example

      structure Foo {
          bar: ShortString
      }

      @length(min: 1, max: 3)
      string ShortString
    """

    val model =
      Model
        .assembler()
        .addUnparsedModel("dynamic.smithy", smithy)
        .discoverModels(this.getClass().getClassLoader())
        .assemble()
        .unwrap()

    val index =
      DynamicSchemaIndex.loadModel(
        model,
        applySchemaRefinements = applySchemaRefinements
      )

    val fooShapeId = ShapeId("example", "Foo")

    val invalidDocument = Document.obj(
      "bar" -> Document.fromString("foobar")
    )
    val schema = index
      .getSchema(fooShapeId)
      .getOrElse(fail("Error: shape missing"))
    Document.Decoder.fromSchema(schema).decode(invalidDocument)
  }

}
