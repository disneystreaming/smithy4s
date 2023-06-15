package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class StringBody(str: String)
object StringBody extends ShapeTag.Companion[StringBody] {
  val id: ShapeId = ShapeId("smithy4s.example", "StringBody")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[StringBody] = struct(
    string.required[StringBody]("str", _.str).addHints(smithy.api.HttpPayload(), smithy.api.Required()),
  ){
    StringBody.apply
  }.withId(id).addHints(hints)
}
