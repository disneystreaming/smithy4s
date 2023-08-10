package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait ForecastResult extends scala.Product with scala.Serializable { self =>
  @inline final def widen: ForecastResult = this
  def $ordinal: Int

  object project {
    def rain: Option[ChanceOfRain] = ForecastResult.RainCase.alt.project.lift(self).map(_.rain)
    def sun: Option[UVIndex] = ForecastResult.SunCase.alt.project.lift(self).map(_.sun)
  }

  def accept[A](visitor: ForecastResult.Visitor[A]): A = this match {
    case value: ForecastResult.RainCase => visitor.rain(value.rain)
    case value: ForecastResult.SunCase => visitor.sun(value.sun)
  }
}
object ForecastResult extends ShapeTag.Companion[ForecastResult] {

  def rain(rain: ChanceOfRain): ForecastResult = RainCase(rain)
  def sun(sun: UVIndex): ForecastResult = SunCase(sun)

  val id: ShapeId = ShapeId("smithy4s.example", "ForecastResult")

  val hints: Hints = Hints.empty

  object optics {
    val rain: Prism[ForecastResult, ChanceOfRain] = Prism.partial[ForecastResult, ChanceOfRain]{ case ForecastResult.RainCase(t) => t }(ForecastResult.RainCase.apply)
    val sun: Prism[ForecastResult, UVIndex] = Prism.partial[ForecastResult, UVIndex]{ case ForecastResult.SunCase(t) => t }(ForecastResult.SunCase.apply)
  }

  final case class RainCase(rain: ChanceOfRain) extends ForecastResult { final def $ordinal: Int = 0 }
  final case class SunCase(sun: UVIndex) extends ForecastResult { final def $ordinal: Int = 1 }

  object RainCase {
    val hints: Hints = Hints.empty
    val schema: Schema[ForecastResult.RainCase] = bijection(ChanceOfRain.schema.addHints(hints), ForecastResult.RainCase(_), _.rain)
    val alt = schema.oneOf[ForecastResult]("rain")
  }
  object SunCase {
    val hints: Hints = Hints.empty
    val schema: Schema[ForecastResult.SunCase] = bijection(UVIndex.schema.addHints(hints), ForecastResult.SunCase(_), _.sun)
    val alt = schema.oneOf[ForecastResult]("sun")
  }

  trait Visitor[A] {
    def rain(value: ChanceOfRain): A
    def sun(value: UVIndex): A
  }

  implicit val schema: Schema[ForecastResult] = union(
    ForecastResult.RainCase.alt,
    ForecastResult.SunCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
