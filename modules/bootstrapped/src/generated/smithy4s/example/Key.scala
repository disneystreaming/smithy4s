package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class Key(key: String)

object Key extends ShapeTag.Companion[Key] {
  val id: ShapeId = ShapeId("smithy4s.example", "Key")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Key] = struct(
    string.required[Key]("key", _.key),
  ){
    Key.apply
  }.withId(id).addHints(hints)
}
