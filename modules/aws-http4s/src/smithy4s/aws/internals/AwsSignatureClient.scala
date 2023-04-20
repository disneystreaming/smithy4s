package smithy4s.aws
package internals

import smithy4s._
import org.http4s._
import org.http4s.client.Client
import cats.effect.Resource
import smithy4s.aws.kernel.AwsCrypto._
import smithy4s.http.internals.URIEncoderDecoder.{encode => uriEncode}
import cats.effect.Concurrent
import fs2.Chunk
import cats.syntax.all._
import org.typelevel.ci.CIString

/**
  * A Client middleware that signs http requests before they are sent to AWS.
  * This works by compiling the body of the request in memory in a chunk before sending
  * it back, which means it is not proper to use it in the context of streaming.
  */
private[aws] object AwsSigningClient {
  def apply[F[_]: Concurrent](
      serviceId: ShapeId,
      endpointId: ShapeId,
      serviceHints: Hints,
      endpointHints: Hints,
      awsEnvironment: AwsEnvironment[F]
  ): Client[F] = Client {
    import awsEnvironment._

    val endpointPrefix = serviceHints
      .get(_root_.aws.api.Service)
      .flatMap(_.endpointPrefix)
      .getOrElse(serviceId.name)
      .toLowerCase()
    val newline = System.lineSeparator()
    val `Content-Type` =
      org.http4s.headers.`Content-Type`.headerInstance.name
    val `Host` = CIString("host")
    val `X-Amz-Date` = CIString("X-Amz-Date")
    val `X-Amz-Security-Token` = CIString("X-Amz-Security-Token")
    val `X-Amz-Target` = CIString("X-Amz-Target")
    val algorithm = "AWS4-HMAC-SHA256"
    def getSignatureKey(
        key: String,
        dateStamp: String,
        regionName: String,
        serviceName: String
    ): Binary = {
      val kSecret = binaryFromString("AWS4" + key)
      val kDate = hmacSha256(dateStamp, kSecret)
      val kRegion = hmacSha256(regionName, kDate)
      val kService = hmacSha256(serviceName, kRegion)
      val kSigning = hmacSha256("aws4_request", kService)
      kSigning
    }

    // scalafmt: { align.preset = most, danglingParentheses.preset = false, maxColumn = 240, align.tokens = [{code = ":"}]}
    (request: Request[F]) => {

      val bodyF = request.body.chunks.compile.to(Chunk).map(_.flatten)
      val awsHeadersF = (bodyF, timestamp, credentials, region).mapN { case (body, timestamp, credentials, region) =>
        val credentialsScope = s"${timestamp.conciseDate}/$region/$endpointPrefix/aws4_request"
        val queryParams: List[(String, String)] =
          request.uri.query.toList.sortBy(_._1).map { case (k, v) => k -> v.getOrElse("") }
        val canonicalQueryString =
          if (queryParams.isEmpty) ""
          else
            queryParams
              .map { case (k, v) =>
                uriEncode(k) + "=" + uriEncode(v)
              }
              .mkString("&")

        // // !\ Important: these must remain in the same order
        val baseHeadersList = List(
          `Content-Type` -> request.contentType.map(_.toString()).orNull,
          `Host` -> request.uri.host.map(_.renderString).orNull,
          `X-Amz-Date` -> timestamp.conciseDateTime,
          `X-Amz-Security-Token` -> credentials.sessionToken.orNull,
          `X-Amz-Target` -> (serviceId.name + "." + endpointId.name)
        ).filterNot(_._2 == null)

        val canonicalHeadersString = baseHeadersList
          .map { case (key, value) =>
            key.toString.toLowerCase + ":" + value.trim
          }
          .mkString(newline)
        lazy val signedHeadersString = baseHeadersList.map(_._1).map(_.toString.toLowerCase()).mkString(";")

        val payloadHash = sha256HexDigest(body.toArray)
        val canonicalRequest = new StringBuilder()
          .append(request.method.name.toUpperCase())
          .append(newline)
          .append(request.uri.path.renderString)
          .append(newline)
          .append(canonicalQueryString)
          .append(newline)
          .append(canonicalHeadersString)
          .append(newline)
          .append(newline)
          .append(signedHeadersString)
          .append(newline)
          .append(payloadHash)
          .result()

        val canonicalRequestHash = sha256HexDigest(canonicalRequest)
        val signatureKey = getSignatureKey(
          credentials.secretAccessKey,
          timestamp.conciseDate,
          region.value,
          endpointPrefix
        )
        val stringToSign = List[String](
          algorithm,
          timestamp.conciseDateTime,
          credentialsScope,
          canonicalRequestHash
        ).mkString(newline)
        val signature = toHexString(hmacSha256(stringToSign, signatureKey))
        val authHeaderValue = s"${algorithm} Credential=${credentials.accessKeyId}/$credentialsScope, SignedHeaders=$signedHeadersString, Signature=$signature"
        val authHeader = Headers("Authorization" -> authHeaderValue)
        val baseHeaders = Headers(baseHeadersList.map { case (k, v) => Header.Raw(k, v) })
        authHeader ++ baseHeaders
      }

      Resource.eval(awsHeadersF).flatMap { headers =>
        httpClient.run(request.transformHeaders(_ ++ headers))
      }
    }

  }

}
