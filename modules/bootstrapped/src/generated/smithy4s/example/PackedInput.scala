package smithy4s.example

import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class PackedInput(key: String)
object PackedInput extends ShapeTag.Companion[PackedInput] {

  val key: FieldLens[PackedInput, String] = string.required[PackedInput]("key", _.key, n => c => c.copy(key = n)).addHints(Required())

  implicit val schema: Schema[PackedInput] = struct(
    key,
  ){
    PackedInput.apply
  }
  .withId(ShapeId("smithy4s.example", "PackedInput"))
}
