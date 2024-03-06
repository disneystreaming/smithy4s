package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EchoInput(pathParam: String, body: EchoBody, queryParam: Option[String] = None)

object EchoInput extends ShapeTag.Companion[EchoInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "EchoInput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(pathParam: String, queryParam: Option[String], body: EchoBody): EchoInput = EchoInput(pathParam, body, queryParam)

  implicit val schema: Schema[EchoInput] = struct(
    string.validated(smithy.api.Length(min = Some(10L), max = None)).required[EchoInput]("pathParam", _.pathParam).addHints(smithy.api.HttpLabel()),
    string.validated(smithy.api.Length(min = Some(10L), max = None)).optional[EchoInput]("queryParam", _.queryParam).addHints(smithy.api.HttpQuery("queryParam")),
    EchoBody.schema.required[EchoInput]("body", _.body).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
