package smithy4s.example

import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class PutStreamedObjectInput(key: String)
object PutStreamedObjectInput extends ShapeTag.$Companion[PutStreamedObjectInput] {
  val $id: ShapeId = ShapeId("smithy4s.example", "PutStreamedObjectInput")

  val $hints: Hints = Hints.empty

  val key: FieldLens[PutStreamedObjectInput, String] = string.required[PutStreamedObjectInput]("key", _.key, n => c => c.copy(key = n)).addHints(Required())

  implicit val $schema: Schema[PutStreamedObjectInput] = struct(
    key,
  ){
    PutStreamedObjectInput.apply
  }.withId($id).addHints($hints)
}