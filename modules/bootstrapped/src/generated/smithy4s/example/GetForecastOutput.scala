package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct

final case class GetForecastOutput(forecast: Option[ForecastResult] = None)
object GetForecastOutput extends ShapeTag.$Companion[GetForecastOutput] {
  val $id: ShapeId = ShapeId("smithy4s.example", "GetForecastOutput")

  val $hints: Hints = Hints.empty

  val forecast: FieldLens[GetForecastOutput, Option[ForecastResult]] = ForecastResult.$schema.optional[GetForecastOutput]("forecast", _.forecast, n => c => c.copy(forecast = n))

  implicit val $schema: Schema[GetForecastOutput] = struct(
    forecast,
  ){
    GetForecastOutput.apply
  }.withId($id).addHints($hints)
}
