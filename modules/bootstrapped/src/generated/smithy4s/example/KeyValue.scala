package smithy4s.example

import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class KeyValue(key: String, value: String)
object KeyValue extends ShapeTag.$Companion[KeyValue] {
  val $id: ShapeId = ShapeId("smithy4s.example", "KeyValue")

  val $hints: Hints = Hints.empty

  val key: FieldLens[KeyValue, String] = string.required[KeyValue]("key", _.key, n => c => c.copy(key = n)).addHints(Required())
  val value: FieldLens[KeyValue, String] = string.required[KeyValue]("value", _.value, n => c => c.copy(value = n)).addHints(Required())

  implicit val $schema: Schema[KeyValue] = struct(
    key,
    value,
  ){
    KeyValue.apply
  }.withId($id).addHints($hints)
}
