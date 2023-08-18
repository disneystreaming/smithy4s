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
package http

import smithy4s.capability.MonadThrowLike
import smithy4s.schema.CachedSchemaCompiler

trait HttpUnaryClientCodecs[F[_], Body, I, E, O] {
  val inputEncoder: HttpRequest.Encoder[Body, I]
  val outputDecoder: HttpResponse.Decoder[F, Body, O]
  val errorDecoder: HttpResponse.Decoder[F, Body, Throwable]
}

object UnaryClientCodecs {

  type For[F[_], Body] = {
    type toKind5[I, E, O, SI, SO] = HttpUnaryClientCodecs[F, Body, I, E, O]
  }

  type Make[F[_], Body] =
    smithy4s.kinds.PolyFunction5[Endpoint.Base, For[F, Body]#toKind5]

  object Make {
    def apply[F[_]: MonadThrowLike, Body](
        input: CachedSchemaCompiler[HttpRequest.Encoder[Body, *]],
        output: CachedSchemaCompiler[HttpResponse.Decoder[F, Body, *]],
        error: CachedSchemaCompiler[HttpResponse.Decoder[F, Body, *]],
        errorDiscriminator: HttpResponse[Body] => F[Option[HttpDiscriminator]],
        toStrict: HttpResponse[Body] => F[(HttpResponse[Body], Blob)]
    ): Make[F, Body] = new Make[F, Body] {

      private val requestEncoderCache: input.Cache = input.createCache()
      private val responseDecoderCache: output.Cache = output.createCache()

      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): HttpUnaryClientCodecs[F, Body, I, E, O] =
        new HttpUnaryClientCodecs[F, Body, I, E, O] {

          val inputEncoder: HttpRequest.Encoder[Body, I] =
            HttpEndpoint.cast(endpoint).toOption match {
              case Some(httpEndpoint) => {
                val httpInputEncoder =
                  HttpRequest.Encoder.fromHttpEndpoint[Body, I](httpEndpoint)
                val requestEncoder =
                  input.fromSchema(endpoint.input, requestEncoderCache)
                httpInputEncoder.pipe(requestEncoder)
              }
              case None => input.fromSchema(endpoint.input, requestEncoderCache)
            }

          val outputDecoder: HttpResponse.Decoder[F, Body, O] =
            output.fromSchema(endpoint.output, responseDecoderCache)
          val errorDecoder: HttpResponse.Decoder[F, Body, Throwable] =
            HttpResponse.Decoder.forErrorAsThrowable(
              endpoint.errorable,
              error,
              errorDiscriminator,
              toStrict
            )
        }
    }
  }

}
