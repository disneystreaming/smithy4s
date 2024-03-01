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
  ).lazily

  // constructor using the original order from the spec
  private def make(weather: String): GetWeatherOutput = GetWeatherOutput(weather)

  implicit val schema: Schema[GetWeatherOutput] = struct(
    string.required[GetWeatherOutput]("weather", _.weather),
  ){
    make
  }.withId(id).addHints(hints)
}
