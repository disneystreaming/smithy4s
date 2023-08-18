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

package smithy4s
package http4s
package internals

import cats.effect.Concurrent
import cats.syntax.all._
import smithy4s.http.HttpRequest
import smithy4s.http.HttpMethod
import smithy4s.http.HttpResponse
import org.http4s.Entity
import org.http4s.Request
import org.http4s.Uri
import org.http4s.client.Client
import smithy4s.http4s.kernel._

// format: off
private[http4s] class SmithyHttp4sClientEndpoint[F[_], I, E, O, SI, SO](
  baseUri: Uri,
  client: Client[F],
  clientCodecs: UnaryClientCodecs[F, I, E, O],
  middleware: Client[F] => Client[F]
)(implicit effect: Concurrent[F]) extends (I => F[O]) {
// format: on

  private val transformedClient: Client[F] = middleware(client)

  def apply(input: I): F[O] = inputToRequest(input).flatMap { request =>
    transformedClient
      .run(request)
      .map(toSmithy4sHttpResponse)
      .use { response =>
        outputFromResponse(response)
      }
  }

  import clientCodecs._

  // Method will be amended by inputEncoder
  val baseRequest = HttpRequest[Entity[F]](
    method = HttpMethod.POST,
    uri = toSmithy4sHttpUri(baseUri),
    headers = Map.empty,
    body = Entity.empty
  )

  def inputToRequest(input: I): F[Request[F]] = {
    fromSmithy4sHttpRequest(inputEncoder.write(baseRequest, input))
  }

  private def outputFromResponse(response: HttpResponse[Entity[F]]): F[O] =
    if (response.isSuccessful)
      outputDecoder.read(response)
    else
      errorDecoder
        .read(response)
        .flatMap(effect.raiseError[O](_))

}
