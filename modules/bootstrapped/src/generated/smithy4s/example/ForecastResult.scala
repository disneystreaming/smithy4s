package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait ForecastResult extends scala.Product with scala.Serializable {
  @inline final def widen: ForecastResult = this
  def $ordinal: Int
}
object ForecastResult extends ShapeTag.Companion[ForecastResult] {

  def rain(rain:ChanceOfRain): ForecastResult = RainCase(rain)
  def sun(sun:UVIndex): ForecastResult = SunCase(sun)

  val id: ShapeId = ShapeId("smithy4s.example", "ForecastResult")

  val hints: Hints = Hints.empty

  object optics {
    val rain: Prism[ForecastResult, ChanceOfRain] = Prism.partial[ForecastResult, ChanceOfRain]{ case RainCase(t) => t }(RainCase.apply)
    val sun: Prism[ForecastResult, UVIndex] = Prism.partial[ForecastResult, UVIndex]{ case SunCase(t) => t }(SunCase.apply)
  }

  final case class RainCase(rain: ChanceOfRain) extends ForecastResult { final def $ordinal: Int = 0 }
  final case class SunCase(sun: UVIndex) extends ForecastResult { final def $ordinal: Int = 1 }

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
    _.$ordinal
  }.withId(id).addHints(hints)
}
