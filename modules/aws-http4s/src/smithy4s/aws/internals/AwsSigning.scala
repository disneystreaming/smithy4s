/*
 *  Copyright 2021-2023 Disney Streaming
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

package smithy4s.aws
package internals

import cats.effect.Concurrent
import cats.effect.Resource
import cats.syntax.all._
import fs2.Chunk
import org.http4s._
import org.http4s.client.Client
import org.typelevel.ci.CIString
import smithy4s._
import smithy4s.aws.kernel.AwsCrypto._

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
  * A Client middleware that signs http requests before they are sent to AWS.
  * This works by compiling the body of the request in memory in a chunk before sending
  * it back, which means it is not proper to use it in the context of streaming.
  */
private[aws] object AwsSigning {

  def middleware[F[_]: Concurrent](
      awsEnvironment: AwsEnvironment[F]
  ): Endpoint.Middleware[Client[F]] = new Endpoint.Middleware[Client[F]] {
    def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
        endpoint: service.Endpoint[_, _, _, _, _]
    ): Client[F] => Client[F] = transformClient(
      service.id,
      endpoint.id,
      service.hints,
      endpoint.hints,
      awsEnvironment
    )
  }

  private def transformClient[F[_]: Concurrent](
      serviceId: ShapeId,
      endpointId: ShapeId,
      serviceHints: Hints,
      endpointHints: Hints,
      awsEnvironment: AwsEnvironment[F]
  ): Client[F] => Client[F] = {
    val endpointPrefix = serviceHints
      .get(_root_.aws.api.Service)
      .flatMap(_.endpointPrefix)
      .getOrElse(serviceId.name)
      .toLowerCase()

    val sign = signingFunction(
      serviceId.name,
      endpointId.name,
      endpointPrefix,
      awsEnvironment.timestamp,
      awsEnvironment.credentials,
      awsEnvironment.region
    )
    client =>
      Client { request =>
        Resource.eval(sign(request)).flatMap { request =>
          client.run(request)
        }
      }
  }

  private[internals] def signingFunction[F[_]: Concurrent](
      serviceName: String,
      operationName: String,
      endpointPrefix: String,
      timestamp: F[Timestamp],
      credentials: F[AwsCredentials],
      region: F[AwsRegion]
  ): Request[F] => F[Request[F]] = {
    val contentType = org.http4s.headers.`Content-Type`.headerInstance
    val `Content-Type` = contentType.name

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
        val queryParams: Vector[(String, String)] =
          request.uri.query.toVector.sorted.map { case (k, v) => k -> v.getOrElse("") }
        val canonicalQueryString =
          if (queryParams.isEmpty) ""
          else
            queryParams
              .map { case (k, v) =>
                URLEncoder.encode(k, StandardCharsets.UTF_8.name()) + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8.name())
              }
              .mkString("&")

        // // !\ Important: these must remain in the same order
        val baseHeadersList = List(
          `Content-Type` -> request.contentType.map(contentType.value(_)).orNull,
          `Host` -> request.uri.host.map(_.renderString).orNull,
          `X-Amz-Date` -> timestamp.conciseDateTime,
          `X-Amz-Security-Token` -> credentials.sessionToken.orNull,
          `X-Amz-Target` -> (serviceName + "." + operationName)
        ).filterNot(_._2 == null)

        val canonicalHeadersString = baseHeadersList
          .map { case (key, value) =>
            key.toString.toLowerCase + ":" + value.trim
          }
          .mkString(newline)
        lazy val signedHeadersString = baseHeadersList.map(_._1).map(_.toString.toLowerCase()).mkString(";")

        val payloadHash = sha256HexDigest(body.toArray)
        val pathString = request.uri.path.toAbsolute.renderString
        val canonicalRequest = new StringBuilder()
          .append(request.method.name.toUpperCase())
          .append(newline)
          .append(pathString)
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

      awsHeadersF.map { headers =>
        request.transformHeaders(_ ++ headers)
      }
    }
  }

  private val newline = System.lineSeparator()
  private val `Host` = CIString("host")
  private val `X-Amz-Date` = CIString("X-Amz-Date")
  private val `X-Amz-Security-Token` = CIString("X-Amz-Security-Token")
  private val `X-Amz-Target` = CIString("X-Amz-Target")
  private val algorithm = "AWS4-HMAC-SHA256"

}
