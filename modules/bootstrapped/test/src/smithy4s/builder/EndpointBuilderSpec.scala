/*
 *  Copyright 2021-2024 Disney Streaming
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

package smithy4s

import munit._

import smithy.api.Documentation
import smithy4s.example.WeatherOperation

class EndpointBuilderSpec extends FunSuite {

  val endpoint = WeatherOperation.GetForecast
  val operation = endpoint.schema

  test(
    "can replace the following values (Id and Hints) using withId and withHints"
  ) {

    val newEndpoint = endpoint.mapSchema {
      _.withId(ShapeId("smithy4s.example", "endpoint"))
        .withHints(Hints(Documentation("this is a endpoint")))
        .withoutError
    }

    assertEquals(newEndpoint.id, ShapeId("smithy4s.example", "endpoint"))
    assertEquals(newEndpoint.hints, Hints(Documentation("this is a endpoint")))
    assertEquals(newEndpoint.error, None)

  }

  test(
    "can modify the following values (Id, Hints, Input, Output, ErrorSchema) using mapId, mapHints, mapInput, mapOutput, mapErrorSchema"
  ) {

    val newEndpoint = endpoint.mapSchema {
      _.mapId(shapeId => ShapeId(shapeId.namespace, "getCityEndpoint"))
        .mapHints { hints =>
          hints ++ Hints(Documentation("new endpoint"))
        }
        .mapInput(s => s.withId(ShapeId("smithy4s.example", "inputSchema")))
        .mapOutput(s => s.withId(ShapeId("smithy4s.example", "outputSchema")))
        .withoutError
    }

    assertEquals(newEndpoint.id, ShapeId("smithy4s.example", "getCityEndpoint"))
    assertEquals(
      newEndpoint.hints,
      Hints(smithy.api.Readonly(), Documentation("new endpoint"))
    )
    assertEquals(
      newEndpoint.input.shapeId,
      ShapeId("smithy4s.example", "inputSchema")
    )
    assertEquals(
      newEndpoint.output.shapeId,
      ShapeId("smithy4s.example", "outputSchema")
    )
    assertEquals(newEndpoint.error, None)
  }
}
