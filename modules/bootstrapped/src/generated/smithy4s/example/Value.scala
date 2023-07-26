package smithy4s.example

import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Value(value: String)
object Value extends ShapeTag.$Companion[Value] {
  val $id: ShapeId = ShapeId("smithy4s.example", "Value")

  val $hints: Hints = Hints.empty

  val value: FieldLens[Value, String] = string.required[Value]("value", _.value, n => c => c.copy(value = n)).addHints(Required())

  implicit val $schema: Schema[Value] = struct(
    value,
  ){
    Value.apply
  }.withId($id).addHints($hints)
}