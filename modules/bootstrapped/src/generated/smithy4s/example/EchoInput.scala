package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EchoInput(pathParam: String, body: EchoBody, queryParam: Option[String] = None)
object EchoInput extends ShapeTag.Companion[EchoInput] {
  val hints: Hints = Hints.empty

  val pathParam = string.validated(smithy.api.Length(min = Some(10L), max = None)).required[EchoInput]("pathParam", _.pathParam).addHints(smithy.api.Required(), smithy.api.HttpLabel())
  val body = EchoBody.schema.required[EchoInput]("body", _.body).addHints(smithy.api.HttpPayload(), smithy.api.Required())
  val queryParam = string.validated(smithy.api.Length(min = Some(10L), max = None)).optional[EchoInput]("queryParam", _.queryParam).addHints(smithy.api.HttpQuery("queryParam"))

  implicit val schema: Schema[EchoInput] = struct(
    pathParam,
    body,
    queryParam,
  ){
    EchoInput.apply
  }.withId(ShapeId("smithy4s.example", "EchoInput")).addHints(hints)
}
