package smithy4s.example.reservedNameOverride

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct

final case class SetOpInput(set: Set)

object SetOpInput extends ShapeTag.Companion[SetOpInput] {
  val id: ShapeId = ShapeId("smithy4s.example.reservedNameOverride", "SetOpInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[SetOpInput] = struct(
    Set.schema.required[SetOpInput]("set", _.set),
  ){
    SetOpInput.apply
  }.withId(id).addHints(hints)
}
