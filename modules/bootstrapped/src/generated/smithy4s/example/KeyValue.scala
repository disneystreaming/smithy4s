package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class KeyValue(key: String, value: String)
object KeyValue extends ShapeTag.Companion[KeyValue] {
  val id: ShapeId = ShapeId("smithy4s.example", "KeyValue")

  val hints: Hints = Hints.empty

  object Optics {
    val key = Lens[KeyValue, String](_.key)(n => a => a.copy(key = n))
    val value = Lens[KeyValue, String](_.value)(n => a => a.copy(value = n))
  }

  implicit val schema: Schema[KeyValue] = struct(
    string.required[KeyValue]("key", _.key).addHints(smithy.api.Required()),
    string.required[KeyValue]("value", _.value).addHints(smithy.api.Required()),
  ){
    KeyValue.apply
  }.withId(id).addHints(hints)
}
