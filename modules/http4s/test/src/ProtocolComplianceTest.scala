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

package smithy4s.compliancetests

import alloy.SimpleRestJson
import cats.effect.{IO, Resource}
import cats.effect.unsafe.IORuntime
import org.http4s._
import smithy4s.{Document, Schema, Service, ShapeId}
import org.http4s.client.Client
import smithy4s.schema.Schema.document
import smithy4s.kinds.FunctorAlgebra
import smithy4s.dynamic.DynamicSchemaIndex
import smithy4s.dynamic.DynamicSchemaIndex.load
import smithy4s.dynamic.model.Model
import smithy4s.http.PayloadError
import smithy4s.http4s.SimpleRestJsonBuilder
import weaver._

object ProtocolComplianceTest extends SimpleIOSuite {
  object SimpleRestJsonIntegration extends Router[IO] with ReverseRouter[IO] {
    type Protocol = SimpleRestJson
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

  private val path = sys.env
    .get("MODEL_DUMP")
    .map(fs2.io.file.Path(_))
    .getOrElse(sys.error("MODEL_DUMP env var not set"))

  private implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global
  private val dynamicSchemaIndexLoader: IO[DynamicSchemaIndex] = fs2.io.file
    .Files[IO]
    .readAll(path)
    .compile
    .toVector
    .map(_.toArray)
    .map(decodeDocument)
    .map(loadDynamic(_).getOrElse(sys.error("unable to load Dynamic path")))

  val pizzaSpec = generateTests(ShapeId("alloy.test", "PizzaAdminService"))

  dynamicSchemaIndexLoader
    .map(
      pizzaSpec(_).foreach(tc =>
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
    )
    .unsafeRunAndForget()

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

  private def decodeDocument(bytes: Array[Byte]): Document = {
    val schema: Schema[Document] = document
    val codecApi = SimpleRestJsonIntegration.codecs
    val codec: codecApi.Codec[Document] = codecApi.compileCodec(schema)
    codecApi
      .decodeFromByteArray[Document](codec, bytes)
      .getOrElse(sys.error("unable to decode smithy model into document"))
  }

  private def loadDynamic(
      doc: Document
  ): Either[PayloadError, DynamicSchemaIndex] = {
    Document.decode[Model](doc).map(load)
  }
}
