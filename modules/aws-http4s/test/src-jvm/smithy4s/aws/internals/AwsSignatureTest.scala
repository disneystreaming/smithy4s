package smithy4s.aws.internals

import cats.Show
import cats.effect.IO
import cats.effect.kernel.Async
import cats.implicits._
import org.http4s.Method
import org.http4s.Request
import org.http4s.Uri
import org.scalacheck.Gen
import org.typelevel.ci.CIString
import smithy4s.Timestamp
import smithy4s.aws.kernel.AwsCredentials
import smithy4s.aws.kernel.AwsRegion
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.signer.Aws4Signer
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams
import software.amazon.awssdk.http.ContentStreamProvider
import software.amazon.awssdk.http.SdkHttpFullRequest
import software.amazon.awssdk.http.SdkHttpMethod
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.utils.StringInputStream
import weaver._
import weaver.scalacheck.CheckConfig
import weaver.scalacheck.Checkers

import java.io.InputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

/**
 * This suite verifies our implementation of the AWS signature algorithm against
 * the java signer from the official AWS SDK.
 */
object AwsSignatureTest extends SimpleIOSuite with Checkers {

  override def checkConfig: CheckConfig =
    super.checkConfig.copy(minimumSuccessful = 1000)

  test("Signature has same behaviour as official AWS") { (_, log) =>
    forall(TestInput.gen) { input =>
      testSignature(input).flatTap { result =>
        {
          val queryParams = input.awsRequest
            .rawQueryParameters()
            .asScala
            .toVector
            .map { case (key, values) =>
              key + ": " + values.asScala.mkString(", ")
            }
            .mkString(System.lineSeparator())
          val report = s"""|
                           |Query params:
                           |$queryParams
                           |""".stripMargin
          log.info(report)
        }.whenA(result.run.isInvalid)
      }
    }
  }

  case class TestInput(
      serviceName: String,
      endpointName: String,
      smithy4sTimestamp: Timestamp,
      smithy4sCredentials: AwsCredentials,
      smithy4sRegion: AwsRegion,
      awsRequest: SdkHttpFullRequest
  )
  object TestInput {
    implicit val show: Show[TestInput] = Show.fromToString

    val genAwsRequest = for {
      httpMethod <- Gen.oneOf(SdkHttpMethod.values().toList)
      host <- Gen.identifier
      path <- Gen.listOf(Gen.identifier).map(_.mkString("/"))
      content <- Gen.asciiStr
      queryParams <- Gen.listOf(Gen.zip(Gen.identifier, Gen.alphaNumStr))
    } yield {
      val builder = SdkHttpFullRequest
        .builder()
        .method(httpMethod)
        .uri(java.net.URI.create(s"https://$host/$path"))
        .contentStreamProvider(new ContentStreamProvider {
          def newStream(): InputStream = new StringInputStream(content)
        })

      queryParams.foreach { case (k, v) =>
        builder.appendRawQueryParameter(k, v)
      }

      builder.build()
    }

    val gen: Gen[TestInput] = for {
      serviceName <- Gen.identifier
      endpointName <- Gen.identifier
      timestamp <- Gen.chooseNum(0L, 4102444800L).map(Timestamp.fromEpochSecond)
      accessKeyId <- Gen.identifier
      secretAccessKey <- Gen.identifier
      accessToken <- Gen.option(Gen.identifier)
      region <- Gen.oneOf(Region.regions().asScala)
      awsRequest <- genAwsRequest
    } yield {
      TestInput(
        serviceName,
        endpointName,
        timestamp,
        AwsCredentials.Default(accessKeyId, secretAccessKey, accessToken),
        AwsRegion(region.id()),
        awsRequest
      )
    }
  }

  def testSignature(
      testInput: TestInput
  ): IO[Expectations] = {
    import testInput._

    val hardClock = new java.time.Clock {
      override def instant(): Instant = smithy4sTimestamp.toInstant
      override def getZone(): ZoneId = ZoneId.of("UTC")
      override def withZone(zone: ZoneId): Clock = this
    }

    val creds = smithy4sCredentials.sessionToken match {
      case None =>
        AwsBasicCredentials.create(
          smithy4sCredentials.accessKeyId,
          smithy4sCredentials.secretAccessKey
        )
      case Some(token) =>
        AwsSessionCredentials.create(
          smithy4sCredentials.accessKeyId,
          smithy4sCredentials.secretAccessKey,
          token
        )
    }

    val region = Region.of(smithy4sRegion.value)

    val params = Aws4SignerParams
      .builder()
      .awsCredentials(creds)
      .signingRegion(region)
      .signingClockOverride(hardClock)
      .signingName(serviceName)
      .build()

    val awsSigner = Aws4Signer.create()
    // Amending the AWS Request to force the AMZ target as it's added automatically
    // by our implementation
    val amendedAwsRequest = awsRequest
      .toBuilder()
      .appendHeader("X-Amz-Target", serviceName + "." + endpointName)
      .build()
    val signedAwsRequest = awsSigner.sign(amendedAwsRequest, params)

    val smithy4sSigner = AwsSigningClient.signingFunction[IO](
      serviceName,
      endpointName,
      serviceName,
      IO(smithy4sTimestamp),
      IO(smithy4sCredentials),
      IO(smithy4sRegion)
    )
    val http4sRequest = toHttp4sRequest(awsRequest)
    smithy4sSigner(http4sRequest).map { signedHttp4sRequest =>
      val maybeExpecations = for {
        awsAuth <- signedAwsRequest
          .headers()
          .get("Authorization")
          .asScala
          .headOption
        smithy4sAuth <- signedHttp4sRequest.headers
          .get(CIString("Authorization"))
          .map(_.head.value)
      } yield expect.eql(smithy4sAuth, awsAuth)

      maybeExpecations.getOrElse(failure("Empty auth"))
    }

  }

  def toHttp4sRequest[Effect[_]: Async](
      request: SdkHttpFullRequest
  ): Request[Effect] = {
    Request(toHttp4sMethod(request.method()))
      .withBodyStream {
        request.contentStreamProvider().toScala match {
          case None => fs2.Stream.empty
          case Some(content) =>
            fs2.io.readInputStream(
              Async[Effect].delay(content.newStream()),
              chunkSize = 512,
              closeAfterUse = true
            )
        }
      }
      .withHeaders(http4sHeaders(request))
      .withUri(Uri.unsafeFromString(request.getUri().toString()))
  }

  def http4sHeaders(request: SdkHttpFullRequest): Vector[(String, String)] =
    request.headers().asScala.toVector.flatMap { case (key, values) =>
      values.asScala.map(key -> _)
    }

  def toHttp4sMethod(method: SdkHttpMethod): Method = method match {
    case SdkHttpMethod.POST    => Method.POST
    case SdkHttpMethod.GET     => Method.GET
    case SdkHttpMethod.PUT     => Method.PUT
    case SdkHttpMethod.DELETE  => Method.DELETE
    case SdkHttpMethod.HEAD    => Method.HEAD
    case SdkHttpMethod.PATCH   => Method.PATCH
    case SdkHttpMethod.OPTIONS => Method.OPTIONS
  }

}
