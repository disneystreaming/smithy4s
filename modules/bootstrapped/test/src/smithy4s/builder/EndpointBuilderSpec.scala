package smithy4s

import munit._

class EndpointBuilderSpec extends FunSuite {

  val endpoint = smithy4s.example.WeatherGen.endpoints.apply(2)

  val builder = smithy4s.Endpoint.Builder.fromEndpoint(endpoint)

  test(
    "can replace the following values (Id and Hints) using withId and withHints"
  ) {

    val newEndpoint = builder
      .withId(ShapeId.apply("smithy4s.example", "endpoint"))
      .withHints(Hints(Hints.Binding.DynamicBinding.apply(ShapeId.apply("smithy.api", "documentation"), Document.fromString("this is a endpoint"))))
      .withErrorable(None)
      .build

    assertEquals(newEndpoint.id, ShapeId.apply("smithy4s.example", "endpoint"))
    assertEquals(newEndpoint.hints, Hints(Hints.Binding.DynamicBinding.apply(ShapeId.apply("smithy.api", "documentation"), Document.fromString("this is a endpoint"))))
    assertEquals(newEndpoint.errorable, None)

  }


  test(
    "can modify the following values (Id, Hints, Input, Output, Errorable) using mapId, mapHints, mapInput, mapOutput, mapErrorable"
  ) {


    val newEndpoint = builder
      .mapId(shapeId => ShapeId.apply(shapeId.namespace, "getCityEndpoint"))
      .mapHints { hints =>
        hints.++(
          Hints(
            Hints.Binding.DynamicBinding.apply(ShapeId.apply("smithy.api", "documentation"), Document.fromString("new endpoint")))
        )
      }
      .mapInput(s => s.withId(ShapeId.apply("smithy4s.example", "inputSchema")))
      .mapOutput(s => s.withId(ShapeId.apply("smithy4s.example", "outputSchema")))
      .mapErrorable(_ => None)
      .build

    assertEquals(newEndpoint.id, ShapeId.apply("smithy4s.example", "getCityEndpoint"))
    assertEquals( newEndpoint.hints,
      Hints(
        smithy.api.Readonly(),
        Hints.Binding.DynamicBinding.apply(ShapeId.apply("smithy.api", "documentation"), Document.fromString("new endpoint"))
      )
    )
    assertEquals(newEndpoint.input.shapeId, ShapeId.apply("smithy4s.example", "inputSchema"))
    assertEquals(newEndpoint.output.shapeId, ShapeId.apply("smithy4s.example", "outputSchema"))
    assertEquals(newEndpoint.errorable, None)
  }

}
