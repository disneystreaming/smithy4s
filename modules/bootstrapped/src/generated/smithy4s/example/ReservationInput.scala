package smithy4s.example

import smithy.api.HttpLabel
import smithy.api.HttpQuery
import smithy.api.Input
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ReservationInput(name: String, town: Option[String] = None)
object ReservationInput extends ShapeTag.Companion[ReservationInput] {

  val name = string.required[ReservationInput]("name", _.name, n => c => c.copy(name = n)).addHints(HttpLabel(), Required())
  val town = string.optional[ReservationInput]("town", _.town, n => c => c.copy(town = n)).addHints(HttpQuery("town"))

  implicit val schema: Schema[ReservationInput] = struct(
    name,
    town,
  ){
    ReservationInput.apply
  }
  .withId(ShapeId("smithy4s.example", "ReservationInput"))
  .addHints(
    Hints(
      Input(),
    )
  )
}
