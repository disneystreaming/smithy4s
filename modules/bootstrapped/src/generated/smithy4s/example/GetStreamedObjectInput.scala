package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

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
