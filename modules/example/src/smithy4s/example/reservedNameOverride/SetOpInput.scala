package smithy4s.example.reservedNameOverride

import smithy4s.Schema
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag

case class SetOpInput(set: _Set)
object SetOpInput extends ShapeTag.Companion[SetOpInput] {
  val id: ShapeId = ShapeId("smithy4s.example.reservedNameOverride", "SetOpInput")

  val hints : Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[SetOpInput] = struct(
    _Set.schema.required[SetOpInput]("set", _.set).addHints(smithy.api.Required()),
  ){
    SetOpInput.apply
  }.withId(id).addHints(hints)
}