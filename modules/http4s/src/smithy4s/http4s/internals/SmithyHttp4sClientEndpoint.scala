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
import org.http4s.HttpApp
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.http4s.client.Client
import smithy4s.http._
import smithy4s.schema.SchemaAlt

/**
  * A construct that encapsulates interprets and a low-level
  * client into a high-level, domain specific function.
  */
// format: off
private[smithy4s] trait SmithyHttp4sClientEndpoint[F[_], Op[_, _, _, _, _], I, E, O, SI, SO] {
  def send(input: I): F[O]
}
// format: on

private[smithy4s] object SmithyHttp4sClientEndpoint {

  def apply[F[_]: EffectCompat, Op[_, _, _, _, _], I, E, O, SI, SO](
      baseUri: Uri,
      clientOrApp: Either[Client[F], HttpApp[F]],
      endpoint: Endpoint[Op, I, E, O, SI, SO],
      entityCompiler: EntityCompiler[F]
  ): Option[SmithyHttp4sClientEndpoint[F, Op, I, E, O, SI, SO]] =
    HttpEndpoint.cast(endpoint).map { httpEndpoint =>
      new SmithyHttp4sClientEndpointImpl[F, Op, I, E, O, SI, SO](
        baseUri,
        clientOrApp,
        endpoint,
        httpEndpoint,
        entityCompiler
      )
    }

}

// format: off
private[smithy4s] class SmithyHttp4sClientEndpointImpl[F[_], Op[_, _, _, _, _], I, E, O, SI, SO](
  baseUri: Uri,
  clientOrApp: Either[Client[F], HttpApp[F]],
  endpoint: Endpoint[Op, I, E, O, SI, SO],
  httpEndpoint: HttpEndpoint[I],
  entityCompiler: EntityCompiler[F]
)(implicit effect: EffectCompat[F]) extends SmithyHttp4sClientEndpoint[F, Op, I, E, O, SI, SO] {
// format: on

  def send(input: I): F[O] = {
    clientOrApp match {
      case Left(client) =>
        client
          .run(inputToRequest(input))
          .use { response =>
            outputFromResponse(response)
          }
      case Right(httpApp) =>
        httpApp.run(inputToRequest(input)).flatMap(outputFromResponse)
    }
  }

  private val method: org.http4s.Method = toHttp4sMethod(httpEndpoint.method)

  private val inputSchema: Schema[I] = endpoint.input
  private val outputSchema: Schema[O] = endpoint.output

  private val inputMetadataEncoder =
    Metadata.Encoder.fromSchema(inputSchema)
  private val inputHasBody =
    Metadata.TotalDecoder.fromSchema(inputSchema).isEmpty
  private implicit val inputEntityEncoder: EntityEncoder[F, I] =
    entityCompiler.compileEntityEncoder(inputSchema)
  private val outputMetadataDecoder =
    Metadata.PartialDecoder.fromSchema(outputSchema)
  private implicit val outputCodec: EntityDecoder[F, BodyPartial[O]] =
    entityCompiler.compilePartialEntityDecoder(outputSchema)

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
    } else baseRequest
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
            implicit val errorCodec =
              entityCompiler.compilePartialEntityDecoder(schema)

            (response: Response[F]) => {
              decodeResponse[A](response, errorMetadataDecoder)
                .flatMap(_.liftTo[F])
                .map(alt.inject)
            }
          }
        }.unsafeCache(allAlternatives.map(Existential.wrap(_)))

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
  ): F[Either[MetadataError, T]] = {
    val headers = getHeaders(response)
    val metadata = Metadata(headers = headers)
    metadataDecoder.total match {
      case Some(totalDecoder) =>
        totalDecoder.decode(metadata).pure[F]
      case None =>
        for {
          metadataPartial <- metadataDecoder.decode(metadata).pure[F]
          bodyPartial <- response.as[BodyPartial[T]]
        } yield metadataPartial.map(_.combine(bodyPartial))
    }
  }
}
