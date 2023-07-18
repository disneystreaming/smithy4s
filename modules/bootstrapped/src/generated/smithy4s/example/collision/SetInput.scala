package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.struct

final case class SetInput(set: Set[String])
object SetInput extends ShapeTag.Companion[SetInput] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "SetInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  object Lenses {
    val set = Lens[SetInput, Set[String]](_.set)(n => a => a.copy(set = n))
  }

  implicit val schema: Schema[SetInput] = struct(
    MySet.underlyingSchema.required[SetInput]("set", _.set).addHints(smithy.api.Required()),
  ){
    SetInput.apply
  }.withId(id).addHints(hints)
}
