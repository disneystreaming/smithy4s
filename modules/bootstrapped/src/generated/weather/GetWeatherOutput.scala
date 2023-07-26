package weather

import smithy.api.Output
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetWeatherOutput(weather: String)
object GetWeatherOutput extends ShapeTag.Companion[GetWeatherOutput] {

  val weather: FieldLens[GetWeatherOutput, String] = string.required[GetWeatherOutput]("weather", _.weather, n => c => c.copy(weather = n)).addHints(Required())

  implicit val schema: Schema[GetWeatherOutput] = struct(
    weather,
  ){
    GetWeatherOutput.apply
  }
  .withId(ShapeId("weather", "GetWeatherOutput"))
  .addHints(
    Output(),
  )
}
