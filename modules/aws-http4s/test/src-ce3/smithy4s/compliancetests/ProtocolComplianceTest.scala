package smithy4s.aws.compliancetests

import alloy.SimpleRestJson
import cats.effect.std.Env
import cats.effect.{IO, Resource}
import org.http4s.client.Client
import org.http4s.{HttpApp, HttpRoutes}
import smithy4s.dynamic.DynamicSchemaIndex.load
import smithy4s.dynamic.model.{IdRef, Model}
import smithy4s.dynamic.DynamicSchemaIndex
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s.kinds.FunctorAlgebra
import smithy4s.http.{CodecAPI, PayloadError}
import smithy4s.schema.Schema.document
import smithy4s.{Document, Schema, Service, ShapeId}
import weaver.{BaseCatsSuite, CatsUnsafeRun, EffectCompat, EffectSuite, Expectations, Test, TestOutcome}

object ProtocolComplianceTest extends EffectSuite[IO] with BaseCatsSuite {

  implicit protected def effectCompat: EffectCompat[IO] = CatsUnsafeRun
  def getSuite: EffectSuite[IO] = this

  def spec(args: List[String]): fs2.Stream[IO, TestOutcome] = {
    fs2.Stream
      .evals(dynamicSchemaIndexLoader.map(pizzaSpec(_)))
      .parEvalMapUnbounded(runInWeaver)
  }

  object SimpleRestJsonIntegration extends Router[IO] with ReverseRouter[IO] {
    type Protocol = aws.protocols.AwsJson1_0
    val protocolTag = aws.protocols.AwsJson1_0.tag

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

  private val pizzaSpec = generateTests(
    ShapeId("aws.protocoltests.restjson", "RestJson")
  )

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
          .clientAndServerTests(SimpleRestJsonIntegration, wrapper.service)
      })
  }
  val whitelist = Set(
    "TestBodyStructure",
    "NoInputAndNoOutput",
    "UnitInputAndOutput",
    "InputAndOutputWithHeaders",
    "HttpRequestWithLabels",
    "HttpRequestWithLabelsAndTimestampFormat",
    "HttpRequestWithGreedyLabelInPath",
    "HttpRequestWithRegexLiteral",
    "ConstantQueryString",
    "ConstantAndVariableQueryString",
    "IgnoreQueryParamsInResponse",
    "OmitsNullSerializesEmptyString",
    "HttpPrefixHeaders",
    "HttpPrefixHeadersInResponse",
    "HttpPayloadWithStructure",
    "HttpResponseCode",
    "JsonEnums",
    "JsonIntEnums",
    "RecursiveShapes",
    "JsonBlobs",
    "DocumentType",
    "DocumentTypeAsPayload",
    "PostPlayerAction",
    "PostUnionWithJsonName",
    "EndpointOperation",
    "EndpointWithHostLabelOperation",
    "TestBodyStructure",
    "TestPayloadStructure",
  ).map(id => IdRef(s"aws.protocoltests.restjson#$id"))

  private def loadDynamic(
                           doc: Document
                         ): Either[PayloadError, DynamicSchemaIndex] = {
    Document.decode[Model](doc).map(_.filter(whitelist)).map(load)
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

  private def runInWeaver(tc: ComplianceTest[IO]): IO[TestOutcome] = Test(
    tc.name,
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
        case Left(e) => weaver.Expectations.Helpers.failure(e.getMessage)
      }
  )

}