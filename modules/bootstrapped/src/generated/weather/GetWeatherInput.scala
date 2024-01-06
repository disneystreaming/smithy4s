package weather

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class GetWeatherInput(city: String)

object GetWeatherInput extends ShapeTag.Companion[GetWeatherInput] {
  val id: ShapeId = ShapeId("weather", "GetWeatherInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[GetWeatherInput] = struct(
    string.required[GetWeatherInput]("city", _.city).addHints(smithy.api.HttpLabel()),
  ){
    GetWeatherInput.apply
  }.withId(id).addHints(hints)
}
