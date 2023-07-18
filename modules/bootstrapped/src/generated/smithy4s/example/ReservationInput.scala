package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ReservationInput(name: String, town: Option[String] = None)
object ReservationInput extends ShapeTag.Companion[ReservationInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ReservationInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  object Lenses {
    val name = Lens[ReservationInput, String](_.name)(n => a => a.copy(name = n))
    val town = Lens[ReservationInput, Option[String]](_.town)(n => a => a.copy(town = n))
  }

  implicit val schema: Schema[ReservationInput] = struct(
    string.required[ReservationInput]("name", _.name).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
    string.optional[ReservationInput]("town", _.town).addHints(smithy.api.HttpQuery("town")),
  ){
    ReservationInput.apply
  }.withId(id).addHints(hints)
}
