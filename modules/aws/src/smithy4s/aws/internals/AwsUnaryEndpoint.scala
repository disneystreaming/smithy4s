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
package aws
package internals

import cats.MonadThrow
import cats.syntax.all._
import schematic.OneOf
import smithy4s.http._

// format: off
private[aws] class AwsUnaryEndpoint[F[_], Op[_, _, _, _, _], I, E, O, SI, SO](
  awsEnv: AwsEnvironment[F],
  signer: AwsSigner[F],
  endpoint: Endpoint[Op, I, E, O, SI, SO],
  codecAPI: CodecAPI
)(implicit F: MonadThrow[F]) { outer =>
// format: on

  private val metadataEncoder = Metadata.Encoder.fromSchema(endpoint.input)
  private val inputHasBody =
    Metadata.TotalDecoder.fromSchema(endpoint.input).isEmpty
  private val inputCodec = codecAPI.compileCodec(endpoint.input)

  private val maybeOutputMetadataDecoder =
    Metadata.TotalDecoder.fromSchema(endpoint.output)
  private val outputCodec = codecAPI.compileCodec(endpoint.output)
  private val getErrorType: HttpResponse => F[Option[String]] =
    AwsErrorTypeDecoder.fromResponse[F](codecAPI)

  private[aws] def toAwsCall(input: I): AwsCall[F, I, E, O, SI, SO] =
    new AwsCall[F, I, E, O, SI, SO] {
      def run(implicit ev: AwsOperationKind.Unary[SI, SO]): F[O] =
        outer.run(input)
    }

  private def run(input: I): F[O] = {
    val metadata = metadataEncoder.encode(input)
    val payload =
      if (inputHasBody) Some(codecAPI.writeToArray(inputCodec, input))
      else None
    signer
      .sign(endpoint.name, metadata, payload)
      .flatMap(awsEnv.httpClient.run)
      .flatMap { response =>
        if (response.statusCode < 400) {
          val maybeOutput = maybeOutputMetadataDecoder match {
            case Some(totalDecoder) => totalDecoder.decode(response.metadata)
            case None =>
              codecAPI.decodeFromByteArray(outputCodec, response.body)
          }
          maybeOutput match {
            case Right(value) => F.pure[O](value)
            case Left(err)    => F.raiseError[O](err)
          }
        } else {
          getErrorType(response).flatMap[O] { maybeDiscriminator =>
            val errAndAlt = for {
              discriminator <- maybeDiscriminator
              err <- endpoint.errorable
              oneOf <- err.error.find(discriminator)
            } yield (err, oneOf)

            errAndAlt match {
              case Some((err, alt)) =>
                processError(err, alt, response)
              case None =>
                F.raiseError(AwsClientError(response.statusCode, response.body))
            }
          }
        }
      }
  }

  private def processError[ErrorType](
      errorable: Errorable[E],
      oneOf: OneOf[Schematic, E, ErrorType],
      response: HttpResponse
  ): F[O] = {
    val schema = oneOf.schema
    val errorMetadataDecoder = Metadata.PartialDecoder.fromSchema(schema)
    val errorCodec = codecAPI.compileCodec(schema)
    decodeResponse(response, errorCodec, errorMetadataDecoder)
      .map(oneOf.inject)
      .map(errorable.unliftError)
      .flatMap(F.raiseError)
  }

  private def decodeResponse[T](
      response: HttpResponse,
      codec: codecAPI.Codec[T],
      metadataDecoder: Metadata.PartialDecoder[T]
  ): F[T] = {
    val metadata = response.metadata
    metadataDecoder.total match {
      case Some(totalDecoder) =>
        F.fromEither(totalDecoder.decode(metadata))
      case None =>
        for {
          metadataPartial <- metadataDecoder.decode(metadata).liftTo[F]
          bodyPartial <-
            codecAPI.decodeFromByteArrayPartial(codec, response.body).liftTo[F]
        } yield metadataPartial.combine(bodyPartial)
    }
  }

}
