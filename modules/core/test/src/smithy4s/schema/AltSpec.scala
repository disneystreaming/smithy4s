package smithy4s.schema
import weaver._
import smithy4s.example._

object AltSpec extends FunSuite {

  test("Alt.WithValue.matchAs: positive case") {

    val instance =
      ForecastResult.RainCase.alt(ForecastResult.RainCase(ChanceOfRain(42.0f)))

    val matched = instance.matchAs(ForecastResult.RainCase.alt)

    assert(matched == Some(instance))
  }

  test("Alt.WithValue.matchAs: negative case") {
    val instance =
      ForecastResult.RainCase.alt(ForecastResult.RainCase(ChanceOfRain(42.0f)))

    val matched = instance.matchAs(ForecastResult.SunCase.alt)

    assert(matched.isEmpty)
  }

  test("Alt.WithValue.matchAs used in a Schematic: first case") {
    val showInstance = ForecastResult.schema.compile(ShowSchematic)

    val result = showInstance.show(ForecastResult.RainCase(ChanceOfRain(42.0f)))

    assert(result == "union case rain: 42.0")
  }

  test("Alt.WithValue.matchAs used in a Schematic: subsequent case") {

    val showInstance = ForecastResult.schema.compile(ShowSchematic)

    val result = showInstance.show(ForecastResult.SunCase(UVIndex(50)))

    assert(result == "union case sun: 50")
  }
}
