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
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.http4s.client.Client
import smithy4s.http4s.kernel._

// format: off
private[http4s] class SmithyHttp4sClientEndpoint[F[_], I, E, O, SI, SO](
  baseUri: Uri,
  client: Client[F],
  endpoint: Endpoint.Base[I, E, O, SI, SO],
  makeClientCodecs: UnaryClientCodecs.Make[F],
  middleware: Client[F] => Client[F]
)(implicit effect: Concurrent[F]) extends (I => F[O]) {
// format: on

  private val transformedClient: Client[F] = middleware(client)

  def apply(input: I): F[O] = {
    transformedClient
      .run(inputToRequest(input))
      .use { response =>
        outputFromResponse(response)
      }
  }

  // format: off
  val clientCodecs = makeClientCodecs(endpoint)
  import clientCodecs._
  // format: on

  // Method will be amended by inputEncoder
  val baseRequest = Request[F](org.http4s.Method.POST, baseUri).withEmptyBody

  def inputToRequest(input: I): Request[F] = {
    inputEncoder.write(baseRequest, input)
  }

  private def outputFromResponse(response: Response[F]): F[O] =
    if (response.status.isSuccess) outputDecoder.read(response)
    else errorDecoder.read(response).flatMap(effect.raiseError[O](_))

}
