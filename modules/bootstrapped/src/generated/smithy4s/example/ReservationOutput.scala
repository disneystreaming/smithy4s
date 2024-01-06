package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class ReservationOutput(message: String)

object ReservationOutput extends ShapeTag.Companion[ReservationOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ReservationOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  implicit val schema: Schema[ReservationOutput] = struct(
    string.required[ReservationOutput]("message", _.message),
  ){
    ReservationOutput.apply
  }.withId(id).addHints(hints)
}
