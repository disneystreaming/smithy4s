package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait ForecastResult extends scala.Product with scala.Serializable {
  @inline final def widen: ForecastResult = this
}
object ForecastResult extends ShapeTag.Companion[ForecastResult] {
  val id: ShapeId = ShapeId("smithy4s.example", "ForecastResult")

  val hints: Hints = Hints.empty

  final case class RainCase(rain: ChanceOfRain) extends ForecastResult
  def rainCase(rainCase:ChanceOfRain): ForecastResult = RainCase(rainCase)
  final case class SunCase(sun: UVIndex) extends ForecastResult
  def sunCase(sunCase:UVIndex): ForecastResult = SunCase(sunCase)

  object RainCase {
    val hints: Hints = Hints.empty
    val schema: Schema[RainCase] = bijection(ChanceOfRain.schema.addHints(hints), RainCase(_), _.rain)
    val alt = schema.oneOf[ForecastResult]("rain")
  }
  object SunCase {
    val hints: Hints = Hints.empty
    val schema: Schema[SunCase] = bijection(UVIndex.schema.addHints(hints), SunCase(_), _.sun)
    val alt = schema.oneOf[ForecastResult]("sun")
  }

  implicit val schema: Schema[ForecastResult] = union(
    RainCase.alt,
    SunCase.alt,
  ){
    case c: RainCase => RainCase.alt(c)
    case c: SunCase => SunCase.alt(c)
  }.withId(id).addHints(hints)
}
