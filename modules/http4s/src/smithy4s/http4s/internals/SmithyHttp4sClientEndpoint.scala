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

import cats.syntax.all._
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.http4s.client.Client
import smithy4s.http._
import smithy4s.http4s.kernel._
import cats.effect.Concurrent

private[http4s] object SmithyHttp4sClientEndpoint {

  def make[F[_]: Concurrent, Op[_, _, _, _, _], I, E, O, SI, SO](
      baseUri: Uri,
      client: Client[F],
      endpoint: Endpoint[Op, I, E, O, SI, SO],
      compilerContext: ClientCodecs[F],
      middleware: Client[F] => Client[F]
  ): Either[HttpEndpoint.HttpEndpointError, I => F[O]] =
    HttpEndpoint.cast(endpoint).flatMap { httpEndpoint =>
      toHttp4sMethod(httpEndpoint.method)
        .leftMap { e =>
          HttpEndpoint.HttpEndpointError(
            "Couldn't parse HTTP method: " + e
          )
        }
        .map { method =>
          new SmithyHttp4sClientEndpointImpl[F, Op, I, E, O, SI, SO](
            baseUri,
            client,
            method,
            endpoint,
            httpEndpoint,
            compilerContext,
            middleware
          )
        }
    }

}

// format: off
private[http4s] class SmithyHttp4sClientEndpointImpl[F[_], Op[_, _, _, _, _], I, E, O, SI, SO](
  baseUri: Uri,
  client: Client[F],
  method: org.http4s.Method,
  endpoint: Endpoint[Op, I, E, O, SI, SO],
  httpEndpoint: HttpEndpoint[I],
  clientCodecs: ClientCodecs[F],
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
  val inputEncoder: RequestEncoder[F, I] = {
    val httpEndpointEncoder = MessageEncoder.fromHttpEndpoint(httpEndpoint)
    val codecsEncoder = clientCodecs.inputEncoder(endpoint.input)
    RequestEncoder.combine(httpEndpointEncoder, codecsEncoder)
  }
  val outputDecoder: ResponseDecoder[F, O] = clientCodecs.outputDecoder(endpoint.output)
  val errorDecoder: ResponseDecoder[F, Throwable] = clientCodecs.errorDecoder(endpoint.errorable)
  // format: on

  def inputToRequest(input: I): Request[F] = {
    val baseRequest = Request[F](method, baseUri).withEmptyBody
    inputEncoder.addToRequest(baseRequest, input)
  }

  private def outputFromResponse(response: Response[F]): F[O] =
    if (response.status.isSuccess) outputDecoder.decodeResponse(response)
    else errorDecoder.decodeResponse(response).flatMap(effect.raiseError[O](_))

}
