package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class KeyValue(key: String, value: String)

object KeyValue extends ShapeTag.Companion[KeyValue] {
  val id: ShapeId = ShapeId("smithy4s.example", "KeyValue")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[KeyValue] = struct(
    string.required[KeyValue]("key", _.key),
    string.required[KeyValue]("value", _.value),
  ){
    KeyValue.apply
  }.withId(id).addHints(hints)
}
