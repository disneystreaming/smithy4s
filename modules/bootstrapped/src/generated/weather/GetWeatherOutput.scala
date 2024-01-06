package weather

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class GetWeatherOutput(weather: String)

object GetWeatherOutput extends ShapeTag.Companion[GetWeatherOutput] {
  val id: ShapeId = ShapeId("weather", "GetWeatherOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  implicit val schema: Schema[GetWeatherOutput] = struct(
    string.required[GetWeatherOutput]("weather", _.weather),
  ){
    GetWeatherOutput.apply
  }.withId(id).addHints(hints)
}
