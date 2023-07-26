package smithy4s.example

import smithy.api.HttpPayload
import smithy.api.Output
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class OptionalOutputOutput(body: Option[String] = None)
object OptionalOutputOutput extends ShapeTag.Companion[OptionalOutputOutput] {

  val body: FieldLens[OptionalOutputOutput, Option[String]] = string.optional[OptionalOutputOutput]("body", _.body, n => c => c.copy(body = n)).addHints(HttpPayload())

  implicit val schema: Schema[OptionalOutputOutput] = struct(
    body,
  ){
    OptionalOutputOutput.apply
  }
  .withId(ShapeId("smithy4s.example", "OptionalOutputOutput"))
  .addHints(
    Output(),
  )
}
