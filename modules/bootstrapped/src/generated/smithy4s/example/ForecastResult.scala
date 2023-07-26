package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait ForecastResult extends scala.Product with scala.Serializable {
  @inline final def widen: ForecastResult = this
  def _ordinal: Int
}
object ForecastResult extends ShapeTag.Companion[ForecastResult] {
  final case class RainCase(rain: ChanceOfRain) extends ForecastResult { final def _ordinal: Int = 0 }
  def rain(rain:ChanceOfRain): ForecastResult = RainCase(rain)
  final case class SunCase(sun: UVIndex) extends ForecastResult { final def _ordinal: Int = 1 }
  def sun(sun:UVIndex): ForecastResult = SunCase(sun)

  object RainCase {
    val schema: Schema[RainCase] = bijection(ChanceOfRain.schema
    .addHints(
      Hints.empty
    )
    , RainCase(_), _.rain)
    val alt = schema.oneOf[ForecastResult]("rain")
  }
  object SunCase {
    val schema: Schema[SunCase] = bijection(UVIndex.schema
    .addHints(
      Hints.empty
    )
    , SunCase(_), _.sun)
    val alt = schema.oneOf[ForecastResult]("sun")
  }

  implicit val schema: Schema[ForecastResult] = union(
    RainCase.alt,
    SunCase.alt,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "ForecastResult"))
  .addHints(
    Hints.empty
  )
}
