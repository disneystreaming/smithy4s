package smithy4s.example.aws

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class DoThingInput()

object DoThingInput extends ShapeTag.Companion[DoThingInput] {
  val id: ShapeId = ShapeId("smithy4s.example.aws", "DoThingInput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "input"), smithy4s.Document.obj()),
  )


  implicit val schema: Schema[DoThingInput] = constant(DoThingInput()).withId(id).addHints(hints)
}
