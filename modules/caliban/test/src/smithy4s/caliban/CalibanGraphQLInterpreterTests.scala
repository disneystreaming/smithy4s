package smithy4s.caliban

import cats.effect.IO
import cats.effect.std.Dispatcher
import io.circe.Json
import io.circe.syntax._
import smithy4s.example.CityId
import smithy4s.example.CitySummary
import smithy4s.example.ListCitiesOutput
import smithy4s.example.Weather
import smithy4s.example.WeatherGen

import CalibanTestUtils._

object CalibanGraphQLInterpreterTests extends weaver.SimpleIOSuite {

  private val weatherImpl: Weather[IO] = new Weather.Default[IO](IO.stub) {
    override def listCities(
        nextToken: Option[String],
        pageSize: Option[Int]
    ): IO[ListCitiesOutput] = IO(
      ListCitiesOutput(
        items = List(
          CitySummary(
            cityId = CityId("1"),
            name = "London"
          ),
          CitySummary(
            cityId = CityId("2"),
            name = "NYC"
          )
        )
      )
    )
  }

  test("Weather service query interpreter") {
    Dispatcher
      .parallel[IO]
      .use { implicit d =>
        testQueryResult(
          weatherImpl,
          """query {
            |  ListCities() {
            |    items {
            |      cityId
            |      name
            |    }
            |  }
            |}""".stripMargin
        )(
          CalibanGraphQLInterpreter.server[WeatherGen, IO]
        )
      }
      .map(
        assert.eql(
          _,
          Json.obj(
            "ListCities" := Json.obj(
              "items" := Json.arr(
                Json.obj(
                  "cityId" := "1",
                  "name" := "London"
                ),
                Json.obj(
                  "cityId" := "2",
                  "name" := "NYC"
                )
              )
            )
          )
        )
      )
  }

}
