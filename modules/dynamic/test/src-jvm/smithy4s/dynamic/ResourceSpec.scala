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
