package smithy4s
package dynamic

import model._

class ResourceSpec() extends munit.FunSuite {

  test("Decode resource") {
    loadDynamicModel("resources.smithy").map { model =>
      val service = model.shapes(IdRef("smithy4s.example#Library"))
      expect.same(
        service,
        Shape.ServiceCase(
          ServiceShape(
            version = None,
            errors = List.empty,
            operations = List.empty,
            resources =
              List(MemberShape(IdRef("smithy4s.example#Publisher"), Map.empty)),
            traits = Map.empty
          )
        )
      )
    }
  }

  test("Compile resource operations as endpoints") {
    loadDynamicModel("resources.smithy").map { model =>
      val compiled = DynamicSchemaIndex.load(model)

      val endpoints =
        compiled.allServices.head.service.endpoints.map(_.id).toSet
      val expectedEndpoints = List("ListPublishers", "GetBook", "BuyBook")
        .map(id => ShapeId.parse(s"smithy4s.example#$id").get)
        .toSet
      expect.same(endpoints, expectedEndpoints)
    }
  }

}
