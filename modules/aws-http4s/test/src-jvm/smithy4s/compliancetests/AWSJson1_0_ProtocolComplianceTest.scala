package smithy4s.aws

import aws.protocols.AwsJson1_0
import cats.effect.std.Env
import cats.effect.{IO, Resource}
import org.http4s.client.Client
import org.http4s.HttpApp
import smithy4s.dynamic.DynamicSchemaIndex.load
import smithy4s.dynamic.model.Model
import smithy4s.dynamic.DynamicSchemaIndex
import smithy4s.kinds.FunctorAlgebra
import smithy4s.http.{CodecAPI, PayloadError}
import smithy4s.schema.Schema.document
import smithy4s.{Document, Schema, Service, ShapeId, ShapeTag}
import weaver._
import cats.syntax.all._
import org.http4s.implicits._
import smithy4s.aws.http4s._
import smithy4s.aws.json.AwsJsonCodecAPI
import smithy4s.compliancetests._

object AWSJson1_0_ProtocolComplianceTest
    extends EffectSuite[IO]
    with BaseCatsSuite {

  implicit protected def effectCompat: EffectCompat[IO] = CatsUnsafeRun
  def getSuite: EffectSuite[IO] = this

  def spec(args: List[String]): fs2.Stream[IO, TestOutcome] = {
    fs2.Stream
      .evals(dynamicSchemaIndexLoader.map(awsJson1_0(_)))
      .parEvalMapUnbounded(runInWeaver)
  }

  object AwsJson1_0_Implementation extends ReverseRouter[IO] {
    type Protocol = aws.protocols.AwsJson1_0
    val protocolTag: ShapeTag[AwsJson1_0] = aws.protocols.AwsJson1_0
    def codecs: smithy4s.http.CodecAPI = new AwsJsonCodecAPI()

    def reverseRoutes[Alg[_[_, _, _, _, _]]](app: HttpApp[IO])(implicit
        service: Service[Alg]
    ): Resource[IO, FunctorAlgebra[Alg, IO]] = {
      service.simpleAwsClient(
        Client.fromHttpApp(app),
        smithy4s.aws.kernel.AwsRegion.AP_EAST_1
      )
    }
  }

  private val awsJson1_0 = generateTests(
    ShapeId("aws.protocoltests.json10", "JsonRpc10")
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
        .map(decodeDocument(_, AwsJson1_0_Implementation.codecs))
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
          .clientTests(
            AwsJson1_0_Implementation,
            wrapper.service,
            uri"https://JsonRpc10.ap-east-1.amazonaws.com/"
          )
      })
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

  private def runInWeaver(tc: ComplianceTest[IO]): IO[TestOutcome] = Test(
    tc.show,
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
        case Left(e) =>
          weaver.Expectations.Helpers.failure(e.getStackTrace.mkString("\n"))
      }
  )

}
