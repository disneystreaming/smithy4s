package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ReservationOutput(message: String)

object ReservationOutput extends ShapeTag.Companion[ReservationOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ReservationOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(message: String): ReservationOutput = ReservationOutput(message)

  implicit val schema: Schema[ReservationOutput] = struct(
    string.required[ReservationOutput]("message", _.message),
  ){
    make
  }.withId(id).addHints(hints)
}
