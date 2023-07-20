package smithy4s


import munit._
import smithy4s.kinds.PolyFunction5
import smithy4s.example.FooServiceGen

class ServiceBuilderSpec extends FunSuite {

  val service = smithy4s.example.FooService

  val builder =  smithy4s.Service.Builder.fromService(service)

  test(
    "can replace the following values (Id, Version and Hints) using withId, withVersion, withHints"
  ) {

    val newService = builder
      .withId(ShapeId.apply("smithy4s.example", "newService"))
      .withHints(Hints(Hints.Binding.DynamicBinding.apply(ShapeId.apply("smithy.api", "documentation"), Document.fromString("this is new Service"))))
      .withVersion("v3")
      .build

    assertEquals(newService.id, ShapeId.apply("smithy4s.example", "newService"))
    assertEquals(newService.hints, Hints(Hints.Binding.DynamicBinding.apply(ShapeId.apply("smithy.api", "documentation"), Document.fromString("this is new Service"))))
    assertEquals(newService.version, "v3")
  }

  test(
    "can modify the following values (Id, Version and Hints) using mapId, mapVersion, mapHints"
  ) {


    val newService = builder
      .mapId(shapeId => ShapeId.apply(shapeId.namespace, "myService"))
      .mapHints{ hints  =>
        hints.++(
          Hints(Hints.Binding.DynamicBinding.apply(ShapeId.apply("smithy.api", "documentation2"), Document.fromString("new Service"))))
      }
      .mapVersion(version => version + "111")
      .build

    assertEquals(newService.id, ShapeId.apply("smithy4s.example", "myService"))
    assertEquals(newService.hints,
      Hints(
        smithy.api.Documentation("The most basics of services\nGetFoo is its only operation"),
        Hints.Binding.DynamicBinding.apply(ShapeId.apply("smithy.api", "documentation2"), Document.fromString("new Service")))
    )
    assertEquals(newService.version, "1.0.0111")
  }

  test(
    "can map over the endpoints in service with .mapEndpoints"
  ) {

    val mapper = new PolyFunction5[FooServiceGen.Endpoint, FooServiceGen.Endpoint] {
      def apply[I, E, O, SI, SO](op: FooServiceGen.Endpoint[I, E, O, SI, SO]): FooServiceGen.Endpoint[I, E, O, SI, SO] =
        Endpoint
          .Builder
          .fromEndpoint(op)
          .withId(ShapeId.apply("smithy4s.example", "operation1"))
          .build
    }
    val newService = builder
      .mapEndpointEach(mapper)
      .build

    assertEquals(newService.endpoints.map(_.id), List(ShapeId.apply("smithy4s.example", "operation1")))
  }


}
