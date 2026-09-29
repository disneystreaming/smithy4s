package smithy4s.example.aws

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class DoRestThingOutput()

object DoRestThingOutput extends ShapeTag.Companion[DoRestThingOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.aws", "DoRestThingOutput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "output"), smithy4s.Document.obj()),
  )


  implicit val schema: Schema[DoRestThingOutput] = constant(DoRestThingOutput()).withId(id).addHints(hints)
}
