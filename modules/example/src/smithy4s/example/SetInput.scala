package smithy4s.example

import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag
import scala.collection.immutable.Set
import smithy4s.Schema

case class SetInput(set: Set[Value])
object SetInput extends ShapeTag.Companion[SetInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "SetInput")

  val hints : Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[SetInput] = struct(
    MySet.underlyingSchema.required[SetInput]("set", _.set).addHints(smithy.api.Required()),
  ){
    SetInput.apply
  }.withId(id).addHints(hints)
}