package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class PutStreamedObjectInput(key: String)

object PutStreamedObjectInput extends ShapeTag.Companion[PutStreamedObjectInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "PutStreamedObjectInput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[PutStreamedObjectInput] = struct(
    string.required[PutStreamedObjectInput]("key", _.key),
  ){
    PutStreamedObjectInput.apply
  }.withId(id).addHints(hints)
}
