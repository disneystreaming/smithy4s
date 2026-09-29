package smithy4s.example.aws

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class DoRestThingInput()

object DoRestThingInput extends ShapeTag.Companion[DoRestThingInput] {
  val id: ShapeId = ShapeId("smithy4s.example.aws", "DoRestThingInput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "input"), smithy4s.Document.obj()),
  )


  implicit val schema: Schema[DoRestThingInput] = constant(DoRestThingInput()).withId(id).addHints(hints)
}
