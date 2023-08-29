package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetStreamedObjectInput(key: String)
object GetStreamedObjectInput extends ShapeTag.Companion[GetStreamedObjectInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetStreamedObjectInput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[GetStreamedObjectInput] = struct(
    string.required[GetStreamedObjectInput]("key", _.key),
  ){
    GetStreamedObjectInput.apply
  }.withId(id).addHints(hints)
}
