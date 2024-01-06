package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.optics.Lens

final case class GetForecastOutput(forecast: Option[ForecastResult] = None)

object GetForecastOutput extends ShapeTag.Companion[GetForecastOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetForecastOutput")

  val hints: Hints = Hints.empty

  object optics {
    val forecast: Lens[GetForecastOutput, Option[ForecastResult]] = Lens[GetForecastOutput, Option[ForecastResult]](_.forecast)(n => a => a.copy(forecast = n))
  }

  implicit val schema: Schema[GetForecastOutput] = struct(
    ForecastResult.schema.optional[GetForecastOutput]("forecast", _.forecast),
  ){
    GetForecastOutput.apply
  }.withId(id).addHints(hints)
}
