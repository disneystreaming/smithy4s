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

final case class EchoInput(pathParam: String, body: EchoBody, queryParam: Option[String] = None)
object EchoInput extends ShapeTag.$Companion[EchoInput] {
  val $id: ShapeId = ShapeId("smithy4s.example", "EchoInput")

  val $hints: Hints = Hints.empty

  val pathParam: FieldLens[EchoInput, String] = string.validated(Length(min = Some(10L), max = None)).required[EchoInput]("pathParam", _.pathParam, n => c => c.copy(pathParam = n)).addHints(Required(), HttpLabel())
  val body: FieldLens[EchoInput, EchoBody] = EchoBody.$schema.required[EchoInput]("body", _.body, n => c => c.copy(body = n)).addHints(HttpPayload(), Required())
  val queryParam: FieldLens[EchoInput, Option[String]] = string.validated(Length(min = Some(10L), max = None)).optional[EchoInput]("queryParam", _.queryParam, n => c => c.copy(queryParam = n)).addHints(HttpQuery("queryParam"))

  implicit val $schema: Schema[EchoInput] = struct(
    pathParam,
    body,
    queryParam,
  ){
    EchoInput.apply
  }.withId($id).addHints($hints)
}