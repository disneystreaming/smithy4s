package smithy4s.example.reservedNameOverride

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class SetOpInput(set: Set)

object SetOpInput extends ShapeTag.Companion[SetOpInput] {
  val id: ShapeId = ShapeId("smithy4s.example.reservedNameOverride", "SetOpInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(set: Set): SetOpInput = SetOpInput(set)

  implicit val schema: Schema[SetOpInput] = struct(
    Set.schema.required[SetOpInput]("set", _.set),
  )(make).withId(id).addHints(hints)
}
