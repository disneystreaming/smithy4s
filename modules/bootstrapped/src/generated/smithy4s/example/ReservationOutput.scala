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
  )

  val message = string.required[ReservationOutput]("message", _.message).addHints(smithy.api.Required())

  implicit val schema: Schema[ReservationOutput] = struct(
    message,
  ){
    ReservationOutput.apply
  }.withId(id).addHints(hints)
}
