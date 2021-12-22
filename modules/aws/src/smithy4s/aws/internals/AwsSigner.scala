/*
 *  Copyright 2021 Disney Streaming
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

import cats.Monad
import cats.syntax.all._
import smithy4s.HasId
import smithy4s.aws.kernel.AwsSignature
import smithy4s.http.CaseInsensitive
import smithy4s.http.HttpMethod
import smithy4s.http.Metadata

private[aws] trait AwsSigner[F[_]] {
  def sign(
      endpointName: String,
      metadata: Metadata,
      body: Option[Array[Byte]]
  ): F[HttpRequest]
}

object AwsSigner {

  private[aws] val EMPTY_PATH = "/"

  private[aws] def rpcSigner[F[_]: Monad](
      serviceId: HasId,
      endpointPrefix: String,
      environment: AwsEnvironment[F],
      contentType: String
  ): AwsSigner[F] =
    new RPCSigner[F](serviceId, endpointPrefix, environment, contentType)

  private class RPCSigner[F[_]: Monad](
      serviceId: HasId,
      endpointPrefix: String,
      awsEnv: AwsEnvironment[F],
      contentType: String
  ) extends AwsSigner[F] {
    def sign(
        endpointName: String,
        metadata: Metadata,
        body: Option[Array[Byte]]
    ): F[HttpRequest] = {
      for {
        r <- awsEnv.region
        c <- awsEnv.credentials
        t <- awsEnv.timestamp
      } yield {
        Request(
          serviceName = serviceId.name,
          operationName = endpointName,
          endpointPrefix = endpointPrefix,
          httpMethod = HttpMethod.POST,
          httpPath = EMPTY_PATH,
          region = r.value,
          accessKeyId = c.accessKeyId,
          secretKey = c.secretAccessKey,
          accessToken = c.sessionToken,
          metadata = metadata,
          timestamp = t,
          body = body,
          contentType = contentType
        )
      }
    }
  }

  /**
  * Low level request model that contains all the information
  * required to fulfil the aws signature algorithm and construct
  * an http request.
  */
  private case class Request(
      serviceName: String,
      operationName: String,
      endpointPrefix: String,
      httpMethod: HttpMethod,
      httpPath: String,
      region: String,
      metadata: Metadata,
      accessKeyId: String,
      secretKey: String,
      accessToken: Option[String],
      timestamp: Timestamp,
      body: Option[Array[Byte]],
      contentType: String
  ) extends HttpRequest
      with AwsSignature.Signable {

    lazy val queryString =
      if (canonicalQueryString == "") "" else s"?$canonicalQueryString"

    val host = s"$endpointPrefix.$region.amazonaws.com"
    val uri: String = s"https://$host$httpPath$queryString"

    /**
    * Associated http headers. Low level representation used as an input
    * to build the high-level reprentation respective to the various
    * http libraries baguette integrates with.
    *
    * This is reponsible for calling the signature algorithm, as the output
    * is used in one of the headers.
    */
    def headers: List[(CaseInsensitive, String)] = {
      val signature = AwsSignature(this)
      val authHeaderValue =
        s"${AwsSignature.algorithm} Credential=$accessKeyId/$credentialsScope, SignedHeaders=$signedHeadersString, Signature=$signature"
      val authHeader = (CaseInsensitive("Authorization") -> authHeaderValue)
      authHeader :: baseHeaders
    }

    lazy val signedHeaders = baseHeaders.map(_._1)
    lazy val signedHeadersString =
      signedHeaders.map(_.toString.toLowerCase).mkString(";")
    def isSigned(header: String): Boolean =
      true // all the base headers are signed in baguette

    lazy val queryParams: List[(String, String)] =
      metadata.queryFlattened.sortBy(_._1).toList

    // !\ Important : these must remain in the same order
    lazy val baseHeaders: List[(CaseInsensitive, String)] = List(
      CaseInsensitive("Content-Type") -> contentType,
      CaseInsensitive("host") -> host,
      CaseInsensitive("X-Amz-Date") -> timestamp.conciseDateTime,
      CaseInsensitive("X-Amz-Security-Token") -> accessToken.orNull,
      CaseInsensitive("X-Amz-Target") -> (serviceName + "." + operationName)
    ).filterNot(_._2 == null)
  }

}
