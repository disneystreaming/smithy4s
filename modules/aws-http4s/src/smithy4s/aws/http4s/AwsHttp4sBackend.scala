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
package http4s

import cats.effect.Async
import cats.effect.Concurrent
import cats.implicits._
import fs2.Chunk
import org.http4s.Header
import org.http4s.Headers
import org.http4s.Method._
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.http4s.client.Client
import org.typelevel.ci.CIString
import smithy4s.aws.SimpleHttpClient
import smithy4s.http.CaseInsensitive
import smithy4s.http.HttpMethod

final class AwsHttp4sBackend[F[_]: Async](client: Client[F])
    extends SimpleHttpClient[F] {

  import AwsHttp4sBackend._
  def run(request: HttpRequest): F[HttpResponse] =
    fromHttpRequest(request).flatMap(client.run(_).use(toHttpResponse[F]))
}

object AwsHttp4sBackend {

  def apply[F[_]: Async](client: Client[F]): SimpleHttpClient[F] =
    new AwsHttp4sBackend(client)

  def fromHttpRequest[F[_]: Async](request: HttpRequest): F[Request[F]] = {
    for {
      headers <- Async[F].delay(request.headers.map { case (k, v) =>
        (CIString(k.toString), v)
      })
      endpoint <- Uri.fromString(request.uri).liftTo[F]
      method = request.httpMethod match {
        case HttpMethod.POST   => POST
        case HttpMethod.GET    => GET
        case HttpMethod.PATCH  => PATCH
        case HttpMethod.PUT    => PUT
        case HttpMethod.DELETE => DELETE
      }
      req = request.body.foldLeft(
        Request[F](
          method = method,
          uri = endpoint,
          headers = Headers(headers.map { case (k, v) => Header.Raw(k, v) })
        )
      )(_.withEntity(_))
    } yield req
  }

  def toHttpResponse[F[_]: Concurrent](
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
