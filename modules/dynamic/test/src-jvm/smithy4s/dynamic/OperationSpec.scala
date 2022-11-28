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
import software.amazon.smithy.model.{Model => SModel}
import software.amazon.smithy.model.shapes.ModelSerializer
import java.nio.file.Paths
import cats.syntax.all._
import http.HttpEndpoint
import DummyIO._

class OperationSpec() extends munit.FunSuite {

  // This is not ideal, but it does the job.
  val cwd = System.getProperty("user.dir");
  val pizzaSpec = Paths.get(cwd + "/sampleSpecs/pizza.smithy").toAbsolutePath()

  test("Decode operation") {
    IO(
      SModel
        .assembler()
        // .discoverModels()
        .addImport(pizzaSpec)
        .assemble()
        .unwrap()
    ).map(ModelSerializer.builder().build.serialize(_))
      .map(NodeToDocument(_))
      .map(smithy4s.Document.decode[smithy4s.dynamic.model.Model](_))
      .flatMap(_.liftTo[IO])
      .map { model =>
        expect(
          model.shapes(IdRef("smithy4s.example#Health")) == Shape.OperationCase(
            OperationShape(
              Some(MemberShape(IdRef("smithy4s.example#HealthRequest"), None)),
              Some(MemberShape(IdRef("smithy4s.example#HealthResponse"), None)),
              Some(
                List(
                  MemberShape(
                    IdRef("smithy4s.example#UnknownServerError"),
                    None
                  )
                )
              ),
              Some(
                Map(
                  IdRef(
                    "smithy.api#http"
                  ) -> Document.obj(
                    "code" -> Document.fromInt(200),
                    "method" -> Document.fromString("GET"),
                    "uri" -> Document.fromString("/health")
                  ),
                  IdRef("smithy.api#readonly") -> Document.obj()
                )
              )
            )
          )
        )
      }
  }

  test("Compile HTTP operation") {
    IO(
      SModel
        .assembler()
        .addImport(pizzaSpec)
        // .discoverModels()
        .assemble()
        .unwrap()
    ).map(ModelSerializer.builder().build.serialize(_))
      .map(NodeToDocument(_))
      .map(smithy4s.Document.decode[smithy4s.dynamic.model.Model](_))
      .flatMap(_.liftTo[IO])
      .map { model =>
        val compiled = DynamicSchemaIndex.load(model)

        val endpoints = compiled.allServices.head.service.endpoints
        val httpEndpoints = endpoints.map(HttpEndpoint.cast(_))

        expect(
          httpEndpoints.forall(_.isRight)
        )
      }
  }

}
