package smithy4s.example.aws

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class DoThingOutput()

object DoThingOutput extends ShapeTag.Companion[DoThingOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.aws", "DoThingOutput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "output"), smithy4s.Document.obj()),
  )


  implicit val schema: Schema[DoThingOutput] = constant(DoThingOutput()).withId(id).addHints(hints)
}
