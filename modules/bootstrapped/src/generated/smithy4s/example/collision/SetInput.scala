package smithy4s.example.collision

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct

final case class SetInput(set: Set[String])

object SetInput extends ShapeTag.Companion[SetInput] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "SetInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[SetInput] = struct(
    MySet.underlyingSchema.required[SetInput]("set", _.set),
  ){
    SetInput.apply
  }.withId(id).addHints(hints)
}
