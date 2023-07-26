package smithy4s.example

import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Key(key: String)
object Key extends ShapeTag.Companion[Key] {

  val key: FieldLens[Key, String] = string.required[Key]("key", _.key, n => c => c.copy(key = n)).addHints(Required())

  implicit val schema: Schema[Key] = struct(
    key,
  ){
    Key.apply
  }
  .withId(ShapeId("smithy4s.example", "Key"))
}
