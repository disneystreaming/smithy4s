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
import cats.effect.Resource
import org.http4s._
import org.http4s.client.Client
import smithy4s.{Document, Schema, Service, ShapeId}
import smithy4s.dynamic.DynamicSchemaIndex
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s.kinds.FunctorAlgebra
import weaver._
import smithy4s.compliancetests.internals._
import cats.syntax.all._
import cats.effect.IO
import cats.effect.std.Env
import smithy4s.compliancetests.AllowRule.{allowRuleDecoder, AllowRules}
import smithy4s.http.PayloadError
import smithy4s.http.CodecAPI
import smithy4s.dynamic.model.Model
import smithy4s.dynamic.DynamicSchemaIndex.load
import smithy4s.schema.Schema.document

/**
  * This suite is NOT implementing MutableFSuite, and uses a higher-level interface
  * to let weaver run it.
  *
  * As a result, it's likely this cannot be run via clicks in IntelliJ, because the tests
  * are dynamically created via effects, which makes it impossible to implement `RunnableSuite`,
  * which is required to run tests via IntelliJ.
  */
object ProtocolComplianceTest extends EffectSuite[IO] with BaseCatsSuite {

  implicit protected def effectCompat: EffectCompat[IO] = CatsUnsafeRun

  def getSuite: EffectSuite[IO] = this

  def spec(args: List[String]): fs2.Stream[IO, TestOutcome] = {
    fs2.Stream
      .eval(dynamicSchemaIndexLoader)
      .fproduct(generateAllowList)
      .flatMap(tuple => fs2.Stream.emits(allTests(tuple._1).map((tuple._2, _))))
      .parEvalMapUnbounded(tuple => runInWeaver(tuple._1, tuple._2))
  }

  object SimpleRestJsonIntegration extends Router[IO] with ReverseRouter[IO] {
    type Protocol = SimpleRestJson
    val protocolTag = alloy.SimpleRestJson

    def codecs = SimpleRestJsonBuilder.codecs

    def routes[Alg[_[_, _, _, _, _]]](
        impl: FunctorAlgebra[Alg, IO]
    )(implicit service: Service[Alg]): Resource[IO, HttpRoutes[IO]] = {
      SimpleRestJsonBuilder(service).routes(impl).resource
    }

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

  private val simpleRestJsonSpec = generateTests(
    ShapeId("aws.protocoltests.restjson", "RestJson")
  )
  private val pizzaSpec = generateTests(
    ShapeId("alloy.test", "PizzaAdminService")
  )

  private val allTests = List(simpleRestJsonSpec, pizzaSpec).combineAll

  private val path = Env
    .make[IO]
    .get("MODEL_DUMP")
    .flatMap(
      _.liftTo[IO](sys.error("MODEL_DUMP env var not set"))
        .map(fs2.io.file.Path(_))
    )

  private val dynamicSchemaIndexLoader: IO[DynamicSchemaIndex] = {
    for {
      p <- path
      dsi <- fs2.io.file
        .Files[IO]
        .readAll(p)
        .compile
        .toVector
        .map(_.toArray)
        .map(decodeDocument(_, SimpleRestJsonIntegration.codecs))
        .flatMap(loadDynamic(_).liftTo[IO])
    } yield dsi
  }

  private def generateTests(
      shapeId: ShapeId
  ): DynamicSchemaIndex => List[ComplianceTest[IO]] = { dynamicSchemaIndex =>
    dynamicSchemaIndex
      .getService(shapeId)
      .toList
      .flatMap(wrapper => {
        HttpProtocolCompliance
          .clientAndServerTests(
            SimpleRestJsonIntegration,
            smithy4s.compliancetests.internals
              .transformService(wrapper.service)(mapAllTimestampsToEpoch)
          )
      })
  }

  private def generateAllowList(dsi: DynamicSchemaIndex): AllowRules = {
    dsi.metadata
      .get("alloyRestJsonAllowList")
      .collect { case Document.DArray(value) =>
        AllowRules(
          value
            .map(allowRuleDecoder(_).getOrElse(sys.error("invalid allow rule")))
            .toVector
        )
      }
      .getOrElse(sys.error("restJsonAllowList not found in metadata"))
  }

  private def loadDynamic(
      doc: Document
  ): Either[PayloadError, DynamicSchemaIndex] = {
    Document.decode[Model](doc).map(load)
  }

  private def decodeDocument(
      bytes: Array[Byte],
      codecApi: CodecAPI
  ): Document = {
    val schema: Schema[Document] = document
    val codec: codecApi.Codec[Document] = codecApi.compileCodec(schema)
    codecApi
      .decodeFromByteArray[Document](codec, bytes)
      .getOrElse(sys.error("unable to decode smithy model into document"))

  }

  private def runInWeaver(
      allowList: AllowRules,
      tc: ComplianceTest[IO]
  ): IO[TestOutcome] = {
    val runner: IO[Expectations] = {
      if (allowList.isAllowed(tc) || tc.show.contains("alloy")) {
        tc.run
          .map(res => expectSuccess(res))
          .attempt
          .map {
            case Right(expectations) => expectations
            case Left(throwable) =>
              Expectations.Helpers.failure(
                s"unexpected error when running test ${throwable.getMessage} \n ${throwable}"
              )
          }

      } else
        tc.run.map(res => expectFailure(res)).attempt.map {
          case Right(expectations) => expectations
          case Left(_)             => Expectations.Helpers.success
        }
    }

    Test(
      tc.show,
      runner
    )
  }

  def expectSuccess(
      res: ComplianceTest.ComplianceResult
  ): Expectations = {
    res.bifoldMap(
      Expectations.Helpers.failure(_),
      _ => Expectations.Helpers.success
    )
  }

  def expectFailure(
      res: ComplianceTest.ComplianceResult
  ): Expectations = {
    res.bifoldMap(
      _ => Expectations.Helpers.success,
      _ =>
        Expectations.Helpers.failure(
          "An expected failure has passed, we might need to update the AllowList  "
        )
    )
  }
}
