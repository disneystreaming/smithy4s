/*
 *  Copyright 2021-2022 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
