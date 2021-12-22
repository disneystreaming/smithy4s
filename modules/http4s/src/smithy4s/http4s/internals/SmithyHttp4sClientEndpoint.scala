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
import schematic.OneOf
import smithy4s.http._
import smithy4s.internals.InputOutput
import smithy4s.syntax._

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

  private val inputSchema: Schema[I] =
    endpoint.input.withHints(InputOutput.Input)
  private val outputSchema: Schema[O] =
    endpoint.output.withHints(InputOutput.Output)
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
    val uri = baseUri.addPath(path).withMultiValueQueryParams(metadata.query)
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
    decodeResponse(response, outputMetadataDecoder)
  }

  private def outputFromErrorResponse(response: Response[F]): F[O] = {
    val errAndAlt = for {
      discriminator <- getFirstHeader(response, errorTypeHeader)
      err <- endpoint.errorable
      oneOf <- err.error.find(discriminator)
    } yield (err, oneOf)

    errAndAlt match {
      case Some((err, alt)) =>
        processError(err, alt, response)
      case None =>
        val headers = toMap(response.headers)
        val code = response.status.code
        response.as[String].flatMap { case body =>
          effect.raiseError(UnknownErrorResponse(code, headers, body))
        }
    }
  }

  private def processError[ErrorType](
      errorable: Errorable[E],
      oneOf: OneOf[Schematic, E, ErrorType],
      response: Response[F]
  ): F[O] = {
    val schema = oneOf.schema
    val errorMetadataDecoder = Metadata.PartialDecoder.fromSchema(schema)
    implicit val errorCodec = entityCompiler.compilePartialEntityDecoder(schema)
    decodeResponse[ErrorType](response, errorMetadataDecoder)
      .map(oneOf.inject)
      .map(errorable.unliftError)
      .flatMap(effect.raiseError)
  }

  private def decodeResponse[T](
      response: Response[F],
      metadataDecoder: Metadata.PartialDecoder[T]
  )(implicit entityDecoder: EntityDecoder[F, BodyPartial[T]]): F[T] = {
    val headers = getHeaders(response)
    val metadata = Metadata(headers = headers)
    metadataDecoder.total match {
      case Some(totalDecoder) =>
        totalDecoder.decode(metadata).liftTo[F]
      case None =>
        for {
          metadataPartial <- metadataDecoder.decode(metadata).liftTo[F]
          bodyPartial <- response.as[BodyPartial[T]]
        } yield metadataPartial.combine(bodyPartial)
    }
  }
}
