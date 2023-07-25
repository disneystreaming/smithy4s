package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class SetInput(set: Set[String])
object SetInput extends ShapeTag.Companion[SetInput] {
  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  val set = MySet.underlyingSchema.required[SetInput]("set", _.set).addHints(smithy.api.Required())

  implicit val schema: Schema[SetInput] = struct(
    set,
  ){
    SetInput.apply
  }.withId(ShapeId("smithy4s.example.collision", "SetInput")).addHints(hints)
}
