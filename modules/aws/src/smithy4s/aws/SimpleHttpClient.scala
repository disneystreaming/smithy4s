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

package smithy4s.aws

import smithy4s.http.CaseInsensitive
import smithy4s.http.HttpMethod
import smithy4s.http.Metadata
import cats.MonadThrow
import cats.syntax.all._
import cats.effect.Concurrent
import fs2.Chunk
import org.http4s._
import org.http4s.Method._

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

  def toHttp4s[F[_]: MonadThrow]: F[Request[F]] = {
    val http4sHeaders: Seq[Header.ToRaw] = headers.map { case (k, v) => (k.toString, v) }

    for {
      endpoint <- Uri.fromString(uri).liftTo[F]
      method <- httpMethod match {
        case HttpMethod.POST         => POST.pure[F]
        case HttpMethod.GET          => GET.pure[F]
        case HttpMethod.PATCH        => PATCH.pure[F]
        case HttpMethod.PUT          => PUT.pure[F]
        case HttpMethod.DELETE       => DELETE.pure[F]
        case HttpMethod.OTHER(value) => Method.fromString(value).liftTo[F]
      }
      req = body
        .foldLeft(
          Request[F](
            method = method,
            uri = endpoint
          )
        )(_.withEntity(_))
        .putHeaders(http4sHeaders: _*)
    } yield req
  }
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

object HttpResponse {

  def fromHttp4s[F[_]: Concurrent](
      response: Response[F]
  ): F[HttpResponse] = {
    val headers = response.headers.headers.toList.map { header =>
      CaseInsensitive(header.name.toString) -> header.value
    }
    response.body.chunks.compile.toVector
      .map(Chunk.concat(_))
      .map(_.toArray)
      .map(HttpResponse(response.status.code, headers, _))
  }

}
