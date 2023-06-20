package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class KeyValue(key: String, value: String)
object KeyValue extends ShapeTag.Companion[KeyValue] {
  val id: ShapeId = ShapeId("smithy4s.example", "KeyValue")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[KeyValue] = struct(
    string.required[KeyValue]("key", _.key).addHints(smithy.api.Required()),
    string.required[KeyValue]("value", _.value).addHints(smithy.api.Required()),
  ){
    KeyValue.apply
  }.withId(id).addHints(hints)
}
