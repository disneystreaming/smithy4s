package weather

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetWeatherInput(city: String)
object GetWeatherInput extends ShapeTag.Companion[GetWeatherInput] {
  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  val city = string.required[GetWeatherInput]("city", _.city).addHints(smithy.api.HttpLabel(), smithy.api.Required())

  implicit val schema: Schema[GetWeatherInput] = struct(
    city,
  ){
    GetWeatherInput.apply
  }.withId(ShapeId("weather", "GetWeatherInput")).addHints(hints)
}
