package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class ReservationInput(name: String, town: Option[String] = None)

object ReservationInput extends ShapeTag.Companion[ReservationInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ReservationInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[ReservationInput] = struct(
    string.required[ReservationInput]("name", _.name).addHints(smithy.api.HttpLabel()),
    string.optional[ReservationInput]("town", _.town).addHints(smithy.api.HttpQuery("town")),
  ){
    ReservationInput.apply
  }.withId(id).addHints(hints)
}
