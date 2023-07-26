package smithy4s.example

import smithy.api.Output
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ReservationOutput(message: String)
object ReservationOutput extends ShapeTag.Companion[ReservationOutput] {

  val message: FieldLens[ReservationOutput, String] = string.required[ReservationOutput]("message", _.message, n => c => c.copy(message = n)).addHints(Required())

  implicit val schema: Schema[ReservationOutput] = struct(
    message,
  ){
    ReservationOutput.apply
  }
  .withId(ShapeId("smithy4s.example", "ReservationOutput"))
  .addHints(
    Output(),
  )
}
