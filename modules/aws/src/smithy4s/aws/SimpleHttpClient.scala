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

import smithy4s.http.CaseInsensitive
import smithy4s.http.HttpMethod
import smithy4s.http.Metadata

/**
  * A low level http-client interface that third parties can implement to
  * to provide back-ends for protocols that are fully request/response-based
  * (as in, do not support streaming operations)
  */
trait SimpleHttpClient[F[_]] {
  def run(request: HttpRequest): F[HttpResponse]
}

trait HttpRequest {
  def httpMethod: HttpMethod
  def uri: String
  def headers: List[(CaseInsensitive, String)]
  def body: Option[Array[Byte]]
}

object HttpRequest {

  final case class Raw(
      httpMethod: HttpMethod,
      uri: String,
      headers: List[(CaseInsensitive, String)] = Nil,
      body: Option[Array[Byte]] = None
  ) extends HttpRequest
}

case class HttpResponse(
    statusCode: Int,
    headers: List[(CaseInsensitive, String)],
    body: Array[Byte]
) extends Metadata.Access {

  def metadata = Metadata(
    headers = headers
      .groupBy(_._1)
      .map { case (k, v) => k -> v.map(_._2).toList }
  )

}
