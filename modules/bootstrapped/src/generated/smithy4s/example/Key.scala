package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Key(key: String)
object Key extends ShapeTag.Companion[Key] {
  val hints: Hints = Hints.empty

  val key = string.required[Key]("key", _.key).addHints(smithy.api.Required())

  implicit val schema: Schema[Key] = struct(
    key,
  ){
    Key.apply
  }.withId(ShapeId("smithy4s.example", "Key")).addHints(hints)
}
