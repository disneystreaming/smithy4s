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
    assertEquals(newEndpoint.errorable, None)

  }

  test(
    "can modify the following values (Id, Hints, Input, Output, Errorable) using mapId, mapHints, mapInput, mapOutput, mapErrorable"
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
    assertEquals(newEndpoint.errorable, None)
  }
}
