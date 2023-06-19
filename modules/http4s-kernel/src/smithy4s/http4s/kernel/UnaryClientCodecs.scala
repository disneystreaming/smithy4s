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

package smithy4s.http4s.kernel

import cats.effect.Concurrent
import org.http4s.Response
import smithy4s.Endpoint
import smithy4s.http.HttpDiscriminator
import smithy4s.http.HttpEndpoint
import smithy4s.schema.CachedSchemaCompiler

trait UnaryClientCodecs[F[_], I, E, O] {
  val inputEncoder: RequestEncoder[F, I]
  val outputDecoder: ResponseDecoder[F, O]
  val errorDecoder: ResponseDecoder[F, Throwable]
}

object UnaryClientCodecs {

  type For[F[_]] = {
    type toKind5[I, E, O, SI, SO] = UnaryClientCodecs[F, I, E, O]
  }

  type Make[F[_]] =
    smithy4s.kinds.PolyFunction5[Endpoint.Base, For[F]#toKind5]

  object Make {
    def apply[F[_]: Concurrent](
        input: CachedSchemaCompiler[RequestEncoder[F, *]],
        output: CachedSchemaCompiler[ResponseDecoder[F, *]],
        error: CachedSchemaCompiler[ResponseDecoder[F, *]],
        errorDiscriminator: Response[F] => F[Option[HttpDiscriminator]]
    ): Make[F] = new Make[F] {

      private val requestEncoderCache: input.Cache = input.createCache()
      private val responseDecoderCache: output.Cache = output.createCache()

      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): UnaryClientCodecs[F, I, E, O] = new UnaryClientCodecs[F, I, E, O] {

        val inputEncoder: RequestEncoder[F, I] =
          HttpEndpoint.cast(endpoint).toOption match {
            case Some(httpEndpoint) => {
              val httpInputEncoder =
                RequestEncoder.fromHttpEndpoint[F, I](httpEndpoint)
              val requestEncoder =
                input.fromSchema(endpoint.input, requestEncoderCache)
              httpInputEncoder.combine(requestEncoder)
            }
            case None => input.fromSchema(endpoint.input, requestEncoderCache)
          }

        val outputDecoder: ResponseDecoder[F, O] =
          output.fromSchema(endpoint.output, responseDecoderCache)
        val errorDecoder: ResponseDecoder[F, Throwable] =
          ResponseDecoder.forErrorAsThrowable(
            endpoint.errorable,
            error,
            errorDiscriminator
          )
      }
    }
  }

}
