package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class SetInput(set: Set[String])

object SetInput extends ShapeTag.Companion[SetInput] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "SetInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(set: Set[String]): SetInput = SetInput(set)

  implicit val schema: Schema[SetInput] = struct(
    MySet.underlyingSchema.required[SetInput]("set", _.set),
  ){
    make
  }.withId(id).addHints(hints)
}
