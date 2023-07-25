package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class TestInput(pathParam: String, body: TestBody, queryParam: Option[String] = None)
object TestInput extends ShapeTag.Companion[TestInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestInput")

  val hints: Hints = Hints.empty

  object Optics {
    val pathParam: Lens[TestInput, String] = Lens[TestInput, String](_.pathParam)(n => a => a.copy(pathParam = n))
    val body: Lens[TestInput, TestBody] = Lens[TestInput, TestBody](_.body)(n => a => a.copy(body = n))
    val queryParam: Lens[TestInput, Option[String]] = Lens[TestInput, Option[String]](_.queryParam)(n => a => a.copy(queryParam = n))
  }

  val pathParam = string.validated(smithy.api.Length(min = Some(10L), max = None)).required[TestInput]("pathParam", _.pathParam).addHints(smithy.api.Required(), smithy.api.HttpLabel())
  val body = TestBody.schema.required[TestInput]("body", _.body).addHints(smithy.api.HttpPayload(), smithy.api.Required())
  val queryParam = string.validated(smithy.api.Length(min = Some(10L), max = None)).optional[TestInput]("queryParam", _.queryParam).addHints(smithy.api.HttpQuery("queryParam"))

  implicit val schema: Schema[TestInput] = struct(
    pathParam,
    body,
    queryParam,
  ){
    TestInput.apply
  }.withId(id).addHints(hints)
}
