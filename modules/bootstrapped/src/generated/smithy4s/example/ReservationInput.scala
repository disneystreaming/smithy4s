package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ReservationInput(name: String, town: Option[String] = None)

object ReservationInput extends ShapeTag.Companion[ReservationInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ReservationInput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "input"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(name: String, town: Option[String]): ReservationInput = ReservationInput(name, town)

  implicit val schema: Schema[ReservationInput] = struct(
    string.required[ReservationInput]("name", _.name).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
    string.optional[ReservationInput]("town", _.town).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("town"))),
  )(make).withId(id).addHints(hints)
}
