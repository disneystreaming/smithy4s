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
            errors = None,
            operations = None,
            resources = Some(
              List(MemberShape(IdRef("smithy4s.example#Publisher"), None))
            ),
            traits = None
          )
        )
      )
    }
  }

  test("Compile resource operations as endpoints") {
    loadDynamicModel("resources.smithy").map { model =>
      val compiled = DynamicSchemaIndex.load(model)

      val endpoints = compiled.allServices.head.service.endpoints
      expect.same(
        endpoints.map(_.id).toSet,
        List("ListPublishers", "GetBook", "BuyBook")
          .map(id => s"smithy4s.example#$id")
          .toSet
      )
    }
  }

}
