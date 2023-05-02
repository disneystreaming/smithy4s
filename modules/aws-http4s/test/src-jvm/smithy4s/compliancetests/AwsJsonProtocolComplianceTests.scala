package smithy4s.aws

import aws.protocols.AwsJson1_0
import aws.protocols.AwsJson1_1
import cats.effect.std.Env
import cats.effect.IO
import smithy4s.dynamic.DynamicSchemaIndex.load
import smithy4s.dynamic.model.Model
import smithy4s.dynamic.DynamicSchemaIndex
import smithy4s.http.{CodecAPI, PayloadError}
import smithy4s.schema.Schema.document
import smithy4s.{Document, Schema, ShapeId}
import weaver._
import cats.syntax.all._
import smithy4s.aws.AwsJson.impl
import smithy4s.aws.json._
import smithy4s.compliancetests._

object AwsJsonProtocolComplianceTests
    extends EffectSuite[IO]
    with BaseCatsSuite {

  implicit protected def effectCompat: EffectCompat[IO] = CatsUnsafeRun
  def getSuite: EffectSuite[IO] = this

  def spec(args: List[String]): fs2.Stream[IO, TestOutcome] = {
    val all: DynamicSchemaIndex => List[ComplianceTest[IO]] = dsi =>
      awsJson1_1(dsi) ++ awsJson1_0(dsi)

    // filtering out Null operation as we dont support sparse yet
    // filtering out HostWithPathOperation as this would be taken-care of by middleware.
    fs2.Stream
      .evals(dynamicSchemaIndexLoader.map(all(_)))
      .filterNot(_.endpoint.name.contains("NullOperation"))
      .filterNot(_.endpoint.name.contains("HostWithPathOperation"))
      .parEvalMapUnbounded(runInWeaver)
  }

  private val codecs = new AwsJsonCodecAPI()

  private val awsJson1_0 = generateTests(
    ShapeId("aws.protocoltests.json10", "JsonRpc10"),
    impl(AwsJson1_0)
  )
  private val awsJson1_1 = generateTests(
    ShapeId("aws.protocoltests.json", "JsonProtocol"),
    impl(AwsJson1_1)
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
        .map(decodeDocument(_, codecs))
        .flatMap(loadDynamic(_).liftTo[IO])
    } yield dsi
  }

  private def generateTests(
      shapeId: ShapeId,
      reverseRouter: ReverseRouter[IO]
  ): DynamicSchemaIndex => List[ComplianceTest[IO]] = { dynamicSchemaIndex =>
    dynamicSchemaIndex
      .getService(shapeId)
      .toList
      .flatMap(wrapper => {
        HttpProtocolCompliance
          .clientTests(
            reverseRouter,
            wrapper.service
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
          weaver.Expectations.Helpers
            .failure(e.getMessage + "\n" + e.getStackTrace.mkString("\n"))
      }
  )

}
