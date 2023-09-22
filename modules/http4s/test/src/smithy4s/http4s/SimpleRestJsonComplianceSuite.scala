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

import alloy.SimpleRestJson
import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all._
import org.http4s._
import org.http4s.client.Client
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.compliancetests._
import smithy4s.dynamic.DynamicSchemaIndex
import smithy4s.http.HttpMediaType
import smithy4s.kinds.FunctorAlgebra
import smithy4s.tests.ProtocolComplianceSuite

/**
  * This suite is NOT implementing MutableFSuite, and uses a higher-level interface
  * to let weaver run it.
  *
  * As a result, it's likely this cannot be run via clicks in IntelliJ, because the tests
  * are dynamically created via effects, which makes it impossible to implement `RunnableSuite`,
  * which is required to run tests via IntelliJ.
  */
object SimpleRestJsonComplianceSuite extends ProtocolComplianceSuite {

  // Filtering these rule because on JS-specific issues,
  // in particular around floating-point precision.
  private val jsDisallowed = Set(
    "RestJsonInputWithHeadersAndAllParams",
    "RestJsonHttpRequestLabelEscaping"
  )

  override def allRules(
      dsi: DynamicSchemaIndex
  ): IO[ComplianceTest[IO] => ShouldRun] = {
    // Decoding borrowed tests
    smithy4s.Document
      .DObject(dsi.metadata)
      .decode[AlloyBorrowedTests]
      .liftTo[IO]
      .map { borrowedTests =>
        borrowedTests.simpleRestJsonBorrowedTests
          .get(ShapeId("aws.protocols", "restJson1"))
          .getOrElse(AllowRules.empty)
          .filterRules(rule =>
            !(weaver.Platform.isJS && jsDisallowed.exists(rule.id.matches))
          )
      }
      .map { decodedRules => (c: ComplianceTest[IO]) =>
        if (c.show.contains("alloy")) ShouldRun.Yes
        else decodedRules.shouldRun(c)
      }

  }

  object SimpleRestJsonIntegration extends Router[IO] with ReverseRouter[IO] {
    type Protocol = SimpleRestJson
    val protocolTag = alloy.SimpleRestJson

    def expectedResponseType(schema: Schema[_]): HttpMediaType = HttpMediaType(
      "application/json"
    )

    def routes[Alg[_[_, _, _, _, _]]](
        impl: FunctorAlgebra[Alg, IO]
    )(implicit service: Service[Alg]): Resource[IO, HttpRoutes[IO]] = {
      SimpleRestJsonBuilder(service).routes(impl).resource
    }

    def reverseRoutes[Alg[_[_, _, _, _, _]]](
        app: HttpApp[IO],
        testHost: Option[String] = None
    )(implicit
        service: Service[Alg]
    ): Resource[IO, FunctorAlgebra[Alg, IO]] = {
      import org.http4s.implicits._
      val baseUri = uri"http://localhost"
      val suppliedHost =
        testHost.map(host => Uri.unsafeFromString(s"http://$host"))
      SimpleRestJsonBuilder(service)
        .client(Client.fromHttpApp(app))
        .uri(suppliedHost.getOrElse(baseUri))
        .resource
    }
  }

  private val simpleRestJsonSpec =
    ShapeId("aws.protocoltests.restjson", "RestJson")

  private val pizzaSpec = ShapeId("alloy.test", "PizzaAdminService")

  override def allTests(dsi: DynamicSchemaIndex) = genClientAndServerTests(
    SimpleRestJsonIntegration,
    simpleRestJsonSpec,
    pizzaSpec
  )(dsi)

  private val modelDump = fileFromEnv("MODEL_DUMP")
  override def dynamicSchemaIndexLoader: IO[DynamicSchemaIndex] = {
    for {
      p <- modelDump
      dsi <- fs2.io.file
        .Files[IO]
        .readAll(p)
        .compile
        .toVector
        .map(_.toArray)
        .map(decodeDocument(_, smithy4s.json.Json.payloadDecoders))
        .flatMap(loadDynamic(_).liftTo[IO])
    } yield dsi
  }

}
