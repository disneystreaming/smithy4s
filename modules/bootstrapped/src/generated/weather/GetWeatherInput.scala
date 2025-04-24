package weather

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetWeatherInput(city: String)

object GetWeatherInput extends ShapeTag.Companion[GetWeatherInput] {
  val id: ShapeId = ShapeId("weather", "GetWeatherInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(city: String): GetWeatherInput = GetWeatherInput(city)

  implicit val schema: Schema[GetWeatherInput] = struct(
    string.required[GetWeatherInput]("city", _.city).addHints(smithy.api.HttpLabel()),
  )(make).withId(id).addHints(hints)
}
