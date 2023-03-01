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

import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.Schema
import smithy4s.Errorable
import org.http4s.Response
import smithy4s.http.HttpDiscriminator
import cats.effect.Async

trait ClientCodecs[F[_]] {

  // format: off
  def inputEncoder[I](schema: Schema[I]) : RequestEncoder[F, I]
  def outputDecoder[O](schema: Schema[O]) : ResponseDecoder[F, O]
  def errorDecoder[E](errorable: Option[Errorable[E]]) : ResponseDecoder[F, Throwable]
  // format: on
}

object ClientCodecs {

  trait Make {
    def makeClientCodecs[F[_]: Async]: ClientCodecs[F]
  }

  def make[F[_]: Async](
      input: CachedSchemaCompiler[RequestEncoder[F, *]],
      output: CachedSchemaCompiler[ResponseDecoder[F, *]],
      error: CachedSchemaCompiler[ResponseDecoder[F, *]],
      errorDiscriminator: Response[F] => F[Option[HttpDiscriminator]]
  ): ClientCodecs[F] =
    new ClientCodecs[F] {
      //format: off
      def inputEncoder[I](schema: Schema[I]): RequestEncoder[F,I] = input.fromSchema(schema, requestEncoderCache)

      def outputDecoder[O](schema: Schema[O]): ResponseDecoder[F,O] = output.fromSchema(schema, responseDecoderCache)

      def errorDecoder[E](errorable: Option[Errorable[E]]): ResponseDecoder[F,Throwable] =
        ResponseDecoder.forErrorAsThrowable(errorable, error, errorDiscriminator)

      val requestEncoderCache: input.Cache = input.createCache()
      val responseDecoderCache: output.Cache = output.createCache()
    }

}
