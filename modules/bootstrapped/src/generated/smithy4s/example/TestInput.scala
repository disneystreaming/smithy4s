package smithy4s.example

import smithy.api.HttpLabel
import smithy.api.HttpPayload
import smithy.api.HttpQuery
import smithy.api.Length
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class TestInput(pathParam: String, body: TestBody, queryParam: Option[String] = None)
object TestInput extends ShapeTag.$Companion[TestInput] {
  val $id: ShapeId = ShapeId("smithy4s.example", "TestInput")

  val $hints: Hints = Hints.empty

  val pathParam: FieldLens[TestInput, String] = string.validated(Length(min = Some(10L), max = None)).required[TestInput]("pathParam", _.pathParam, n => c => c.copy(pathParam = n)).addHints(Required(), HttpLabel())
  val body: FieldLens[TestInput, TestBody] = TestBody.$schema.required[TestInput]("body", _.body, n => c => c.copy(body = n)).addHints(HttpPayload(), Required())
  val queryParam: FieldLens[TestInput, Option[String]] = string.validated(Length(min = Some(10L), max = None)).optional[TestInput]("queryParam", _.queryParam, n => c => c.copy(queryParam = n)).addHints(HttpQuery("queryParam"))

  implicit val $schema: Schema[TestInput] = struct(
    pathParam,
    body,
    queryParam,
  ){
    TestInput.apply
  }.withId($id).addHints($hints)
}