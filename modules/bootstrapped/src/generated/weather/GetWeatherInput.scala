package weather

import smithy.api.HttpLabel
import smithy.api.Input
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetWeatherInput(city: String)
object GetWeatherInput extends ShapeTag.$Companion[GetWeatherInput] {
  val $id: ShapeId = ShapeId("weather", "GetWeatherInput")

  val $hints: Hints = Hints(
    Input(),
  )

  val city: FieldLens[GetWeatherInput, String] = string.required[GetWeatherInput]("city", _.city, n => c => c.copy(city = n)).addHints(HttpLabel(), Required())

  implicit val $schema: Schema[GetWeatherInput] = struct(
    city,
  ){
    GetWeatherInput.apply
  }.withId($id).addHints($hints)
}
