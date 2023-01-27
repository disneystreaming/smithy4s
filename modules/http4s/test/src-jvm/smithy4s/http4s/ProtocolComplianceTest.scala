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

package smithy4s.http4s

import cats.effect.IO
import cats.effect.Resource
import org.http4s._
import org.http4s.client.Client
import smithy4s.Document
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.compliancetests._
import smithy4s.dynamic.DynamicSchemaIndex
import smithy4s.dynamic.DynamicSchemaIndex.load
import smithy4s.dynamic.NodeToDocument
import smithy4s.http.PayloadError
import smithy4s.kinds.FunctorAlgebra
import software.amazon.smithy.model._
import software.amazon.smithy.model.shapes.ModelSerializer
import weaver._

object ProtocolComplianceTest extends SimpleIOSuite {
  object SimpleRestJsonIntegration extends Router[IO] with ReverseRouter[IO] {
    type Protocol = alloy.SimpleRestJson
    val protocolTag = alloy.SimpleRestJson

    def codecs = SimpleRestJsonBuilder.codecs

    def routes[Alg[_[_, _, _, _, _]]](
        impl: FunctorAlgebra[Alg, IO]
    )(implicit service: Service[Alg]): Resource[IO, HttpRoutes[IO]] =
      SimpleRestJsonBuilder(service).routes(impl).resource

    def reverseRoutes[Alg[_[_, _, _, _, _]]](app: HttpApp[IO])(implicit
        service: Service[Alg]
    ): Resource[IO, FunctorAlgebra[Alg, IO]] = {
      import org.http4s.implicits._
      val baseUri = uri"http://localhost/"

      SimpleRestJsonBuilder(service)
        .client(Client.fromHttpApp(app))
        .uri(baseUri)
        .resource
    }
  }

  private val smithyModel = Model.assembler().discoverModels.assemble.unwrap()
  private val node =
    ModelSerializer.builder().build.serialize(smithyModel)
  private val doc = NodeToDocument(node)
  private val dynamicSchemaIndex: DynamicSchemaIndex =
    loadDynamic(doc).getOrElse(sys.error("unable to load Dynamic model"))

  val pizzaSpec = generateTests(ShapeId("alloy.test", "PizzaAdminService"))

  pizzaSpec(dynamicSchemaIndex).foreach(tc =>
    test(tc.name) {
      tc.run
        .map[Expectations] {
          case Left(value) =>
            Expectations.Helpers.failure(value)
          case Right(_) =>
            Expectations.Helpers.success
        }
        .attempt
        .map {
          case Right(expectations) => expectations
          case Left(e)             => failure(e.getMessage)
        }
    }
  )

  private def generateTests(
      shapeId: ShapeId
  ): DynamicSchemaIndex => List[ComplianceTest[IO]] = { dynamicSchemaIndex =>
    dynamicSchemaIndex
      .getService(shapeId)
      .toList
      .flatMap(wrapper => {
        HttpProtocolCompliance
          .clientAndServerTests(SimpleRestJsonIntegration, wrapper.service)
      })
  }

  private def loadDynamic(
      doc: Document
  ): Either[PayloadError, DynamicSchemaIndex] = {
    Document.decode[smithy4s.dynamic.model.Model](doc).map(load)
  }
}
