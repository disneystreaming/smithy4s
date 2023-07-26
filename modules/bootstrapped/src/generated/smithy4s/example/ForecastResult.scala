package smithy4s.example

import smithy4s.Bijection
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
object ForecastResult extends ShapeTag.$Companion[ForecastResult] {

  def rain(rain:ChanceOfRain): ForecastResult = RainCase(rain)
  def sun(sun:UVIndex): ForecastResult = SunCase(sun)

  val $id: ShapeId = ShapeId("smithy4s.example", "ForecastResult")

  val $hints: Hints = Hints.empty

  final case class RainCase(rain: ChanceOfRain) extends ForecastResult { final def _ordinal: Int = 0 }
  final case class SunCase(sun: UVIndex) extends ForecastResult { final def _ordinal: Int = 1 }

  object RainCase {
    implicit val fromValue: Bijection[ChanceOfRain, RainCase] = Bijection(RainCase(_), _.rain)
    implicit val toValue: Bijection[RainCase, ChanceOfRain] = fromValue.swap
    val $schema: Schema[RainCase] = bijection(ChanceOfRain.$schema, fromValue)
  }
  object SunCase {
    implicit val fromValue: Bijection[UVIndex, SunCase] = Bijection(SunCase(_), _.sun)
    implicit val toValue: Bijection[SunCase, UVIndex] = fromValue.swap
    val $schema: Schema[SunCase] = bijection(UVIndex.$schema, fromValue)
  }

  val rain = RainCase.$schema.oneOf[ForecastResult]("rain")
  val sun = SunCase.$schema.oneOf[ForecastResult]("sun")

  implicit val $schema: Schema[ForecastResult] = union(
    rain,
    sun,
  ){
    _._ordinal
  }.withId($id).addHints($hints)
}
