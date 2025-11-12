package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class Nil()

object Nil extends ShapeTag.Companion[Nil] {
  val id: ShapeId = ShapeId("smithy4s.example", "Nil")

  val hints: Hints = Hints.empty


  implicit val schema: Schema[Nil] = constant(Nil()).withId(id).addHints(hints)
}
