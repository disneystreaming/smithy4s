package weather

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetWeatherOutput(weather: String)
object GetWeatherOutput extends ShapeTag.Companion[GetWeatherOutput] {
  val id: ShapeId = ShapeId("weather", "GetWeatherOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  val weather = string.required[GetWeatherOutput]("weather", _.weather).addHints(smithy.api.Required())

  implicit val schema: Schema[GetWeatherOutput] = struct(
    weather,
  ){
    GetWeatherOutput.apply
  }.withId(id).addHints(hints)
}
