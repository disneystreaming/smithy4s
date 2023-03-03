package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

case class Endpoint()
object Endpoint extends ShapeTag.Companion[Endpoint] {
  val id: ShapeId = ShapeId("smithy4s.example", "Endpoint")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Endpoint] = constant(Endpoint()).withId(id).addHints(hints)
}