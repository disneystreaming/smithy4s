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
import org.http4s.EntityDecoder
import org.http4s.EntityEncoder
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.http4s.client.Client
import scodec.bits.ByteVector
import smithy4s.http._
import smithy4s.schema.SchemaAlt
import smithy4s.http4s.kernel._
import cats.effect.Concurrent

/**
  * A construct that encapsulates interprets and a low-level
  * client into a high-level, domain specific function.
  */
// format: off
private[http4s] trait SmithyHttp4sClientEndpoint[F[_], Op[_, _, _, _, _], I, E, O, SI, SO] {
  def send(input: I): F[O]
}
// format: on

private[http4s] object SmithyHttp4sClientEndpoint {

  def make[F[_]: Concurrent, Op[_, _, _, _, _], I, E, O, SI, SO](
      baseUri: Uri,
      client: Client[F],
      endpoint: Endpoint[Op, I, E, O, SI, SO],
      compilerContext: CompilerContext[F],
      middleware: Client[F] => Client[F]
  ): Either[
    HttpEndpoint.HttpEndpointError,
    SmithyHttp4sClientEndpoint[F, Op, I, E, O, SI, SO]
  ] =
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
  compilerContext: CompilerContext[F],
  middleware: Client[F] => Client[F]
)(implicit effect: Concurrent[F]) extends SmithyHttp4sClientEndpoint[F, Op, I, E, O, SI, SO] {
// format: on

  private val transformedClient: Client[F] = middleware(client)

  def send(input: I): F[O] = {
    transformedClient
      .run(inputToRequest(input))
      .use { response =>
        outputFromResponse(response)
      }
  }

  import compilerContext._
  private val inputSchema: Schema[I] = endpoint.input
  private val outputSchema: Schema[O] = endpoint.output

  private val inputMetadataEncoder =
    Metadata.Encoder.fromSchema(inputSchema)
  private val inputHasBody =
    Metadata.TotalDecoder.fromSchema(inputSchema, metadataDecoderCache).isEmpty
  private implicit val inputEntityEncoder: EntityEncoder[F, I] =
    entityCompiler.compileEntityEncoder(inputSchema, entityCache)
  private implicit val outputDecoder: EntityDecoder[F, O] =
    entityCompiler.compileEntityDecoder(outputSchema, entityCache)
  private val errorDecoder: ErrorDecoder[F, E] = {
    val alts =
      endpoint.errorable.map(_.error.alternatives).getOrElse(Vector.empty)
    val errorAltPicker =
      new ErrorAltPicker(alts, CaseInsensitive(errorTypeHeader))
    def discriminate(response: Response[F]): F[Option[SchemaAlt[E, _]]] = {
      val code = response.status.code
      val headers = getHeaders(response)
      effect.pure(errorAltPicker(code, headers))
    }
    ErrorResponseDecoder.compile(endpoint.errorable, entityCompiler, discriminate)
  }

  def inputToRequest(input: I): Request[F] = {
    val metadata = inputMetadataEncoder.encode(input)
    val path = httpEndpoint.path(input)
    val staticQueries = httpEndpoint.staticQueryParams
    val uri = baseUri
      .copy(path = baseUri.path.addSegments(path.map(Uri.Path.Segment(_))))
      .withMultiValueQueryParams(staticQueries ++ metadata.query)
    val headers = toHeaders(metadata.headers)
    val baseRequest = Request[F](method, uri, headers = headers)
    if (inputHasBody) {
      baseRequest.withEntity(input)
    } else baseRequest.withEntity(ByteVector.empty)
  }

  private def outputFromResponse(response: Response[F]): F[O] =
    if (response.status.isSuccess) outputFromSuccessResponse(response)
    else outputFromErrorResponse(response)

  private def outputFromSuccessResponse(response: Response[F]): F[O] = {
    response.as[O]
  }

  private def outputFromErrorResponse(response: Response[F]): F[O] = {
    errorDecoder
      .decodeErrorAsThrowable(response)
      .flatMap(effect.raiseError[O](_))
  }

}
