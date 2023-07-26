package weather

import smithy.api.HttpLabel
import smithy.api.Input
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetWeatherInput(city: String)
object GetWeatherInput extends ShapeTag.Companion[GetWeatherInput] {

  val city: FieldLens[GetWeatherInput, String] = string.required[GetWeatherInput]("city", _.city, n => c => c.copy(city = n)).addHints(HttpLabel(), Required())

  implicit val schema: Schema[GetWeatherInput] = struct(
    city,
  ){
    GetWeatherInput.apply
  }
  .withId(ShapeId("weather", "GetWeatherInput"))
  .addHints(
    Input(),
  )
}
