package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetStreamedObjectInput(key: String)
object GetStreamedObjectInput extends ShapeTag.Companion[GetStreamedObjectInput] {
  val hints: Hints = Hints.empty

  val key = string.required[GetStreamedObjectInput]("key", _.key).addHints(smithy.api.Required())

  implicit val schema: Schema[GetStreamedObjectInput] = struct(
    key,
  ){
    GetStreamedObjectInput.apply
  }.withId(ShapeId("smithy4s.example", "GetStreamedObjectInput")).addHints(hints)
}
