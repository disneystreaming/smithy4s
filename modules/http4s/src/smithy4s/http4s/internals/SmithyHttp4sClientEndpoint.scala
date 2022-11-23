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
import smithy4s.kinds._
import smithy4s.http._
import smithy4s.schema.SchemaAlt

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

  def make[F[_]: EffectCompat, Op[_, _, _, _, _], I, E, O, SI, SO](
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
)(implicit effect: EffectCompat[F]) extends SmithyHttp4sClientEndpoint[F, Op, I, E, O, SI, SO] {
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
  private val outputMetadataDecoder =
    Metadata.PartialDecoder.fromSchema(outputSchema, metadataDecoderCache)
  private implicit val outputCodec: EntityDecoder[F, BodyPartial[O]] =
    entityCompiler.compilePartialEntityDecoder(outputSchema, entityCache)

  def inputToRequest(input: I): Request[F] = {
    val metadata = inputMetadataEncoder.encode(input)
    val path = httpEndpoint.path(input)
    val uri = baseUri
      .copy(path = baseUri.path.addSegments(path.map(Uri.Path.Segment(_))))
      .withMultiValueQueryParams(metadata.query)
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
    decodeResponse(response, outputMetadataDecoder).rethrow
  }

  private def errorResponseFallBack(response: Response[F]): F[O] = {
    val headers = toMap(response.headers)
    val code = response.status.code
    response.as[String].flatMap { case body =>
      effect.raiseError(UnknownErrorResponse(code, headers, body))
    }
  }

  private def getErrorDiscriminator(response: Response[F]) = {
    getFirstHeader(response, errorTypeHeader)
      .map(errorType =>
        ShapeId
          .parse(errorType)
          .map(ErrorAltPicker.ErrorDiscriminator.FullId(_))
          .getOrElse(ErrorAltPicker.ErrorDiscriminator.NameOnly(errorType))
      )
      .getOrElse(
        ErrorAltPicker.ErrorDiscriminator.StatusCode(response.status.code)
      )
  }

  private val outputFromErrorResponse: Response[F] => F[O] = {
    endpoint.errorable match {
      case None => errorResponseFallBack(_)
      case Some(err) =>
        val allAlternatives = err.error.alternatives
        val picker = new ErrorAltPicker(allAlternatives)
        type ErrorDecoder[A] = Response[F] => F[E]
        val decodeFunction = new PolyFunction[SchemaAlt[E, *], ErrorDecoder] {
          def apply[A](alt: SchemaAlt[E, A]): Response[F] => F[E] = {
            val schema = alt.instance
            val errorMetadataDecoder =
              Metadata.PartialDecoder.fromSchema(schema)
            // TODO : apply proper memoization of error instances/
            // In the line below, we create a new, ephemeral cache for the dynamic recompilation of the error schema.
            // This is because the "compile entity encoder" method can trigger a transformation of hints, which
            // lead to cache-miss and would lead to new entries in existing cache, effectively leading to a memory leak.
            val ephemeralEntityCache = entityCompiler.createCache()
            implicit val errorCodec =
              entityCompiler.compilePartialEntityDecoder(
                schema,
                ephemeralEntityCache
              )

            (response: Response[F]) => {
              decodeResponse[A](response, errorMetadataDecoder)
                .flatMap(_.liftTo[F])
                .map(alt.inject)
            }
          }
        }.unsafeCacheBy(allAlternatives.map(Kind1.existential(_)), identity(_))

        (response: Response[F]) => {
          val discriminator = getErrorDiscriminator(response)
          picker.getPreciseAlternative(discriminator) match {
            case None => errorResponseFallBack(response)
            case Some(alt) =>
              decodeFunction(alt)(response)
                .map(err.unliftError)
                .flatMap(effect.raiseError)
          }
        }
    }
  }

  private def decodeResponse[T](
      response: Response[F],
      metadataDecoder: Metadata.PartialDecoder[T]
  )(implicit
      entityDecoder: EntityDecoder[F, BodyPartial[T]]
  ): F[Either[HttpContractError, T]] = {
    val headers = getHeaders(response)
    val metadata =
      Metadata(headers = headers, statusCode = Some(response.status.code))
    metadataDecoder.total match {
      case Some(totalDecoder) =>
        totalDecoder.decode(metadata).pure[F].widen
      case None =>
        for {
          metadataPartial <- metadataDecoder.decode(metadata).pure[F]
          bodyPartial <- response.as[BodyPartial[T]]
        } yield metadataPartial.flatMap(_.combineCatch(bodyPartial))
    }
  }
}
