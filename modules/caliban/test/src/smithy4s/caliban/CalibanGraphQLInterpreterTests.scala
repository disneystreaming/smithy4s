package smithy4s.caliban

import caliban.CalibanError
import caliban.GraphQLInterpreter
import caliban.RootResolver
import caliban.interop.cats.implicits._
import caliban.schema.Schema
import cats.effect.IO
import cats.effect.Resource
import cats.effect.std.Dispatcher
import smithy4s.example.CityId
import smithy4s.example.CitySummary
import smithy4s.example.ListCitiesOutput
import smithy4s.example.Weather
import smithy4s.example.WeatherGen
import io.circe.syntax._

object CalibanGraphQLInterpreterTests extends weaver.IOSuite {
  private implicit val rt: zio.Runtime[Any] = zio.Runtime.default

  override type Res = GraphQLInterpreter[Any, CalibanError]
  override def sharedResource: Resource[IO, Res] =
    Dispatcher
      .parallel[IO]
      .evalMap { implicit d =>
        implicit val schema: Schema[Any, Weather[IO]] =
          CalibanGraphQLInterpreter.server[WeatherGen, IO]

        val api = caliban.graphQL(RootResolver(impl: Weather[IO]))

        // todo
        // println(api.render)
        api.interpreterAsync[IO]
      }

  val impl = new Weather.Default[IO](IO.stub) {
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

  test("demo") { interp =>
    val query = """query {
      ListCities() {
        items {
          cityId
          name
        }
      }
    }"""

    interp
      .executeAsync[IO](query)
      .map { resp =>
        val expected = Map(
          "ListCities" := Map(
            "items" := List(
              Map(
                "cityId" := "1",
                "name" := "London"
              ),
              Map(
                "cityId" := "2",
                "name" := "NYC"
              )
            )
          )
        ).asJson

        assert.same(resp.data.asJson, expected)
      }
  }

}
