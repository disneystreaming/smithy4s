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
import cats.effect.Async

trait ServerCodecs[F[_]] {
  // format: off
  def inputDecoder[I](schema: Schema[I]) : RequestDecoder[F, I]
  def outputEncoder[O](schema: Schema[O]) : ResponseEncoder[F, O]
  def errorEncoder[E](schema: Schema[E]) : ResponseEncoder[F, E]
  def errorEncoder[E](errorable: Option[Errorable[E]]) : ResponseEncoder[F, E]
  // format: on
}

object ServerCodecs {

  trait Make {
    def makeServerCodecs[F[_]: Async]: ServerCodecs[F]
  }

  def make[F[_]](
      input: CachedSchemaCompiler[RequestDecoder[F, *]],
      output: CachedSchemaCompiler[ResponseEncoder[F, *]],
      error: CachedSchemaCompiler[ResponseEncoder[F, *]]
  ): ServerCodecs[F] =
    new ServerCodecs[F] {
      //format: off
      def inputDecoder[I](schema: Schema[I]): RequestDecoder[F,I] =
        input.fromSchema(schema, requestDecoderCache)
      def outputEncoder[O](schema: Schema[O]): ResponseEncoder[F,O] =
        output.fromSchema(schema, responseEncoderCache)
      def errorEncoder[E](schema: Schema[E]) : ResponseEncoder[F, E] =
        error.fromSchema(schema, errorResponseEncoderCache)
      def errorEncoder[E](errorable: Option[Errorable[E]]): ResponseEncoder[F,E] =
         ResponseEncoder.forError(smithy4s.errorTypeHeader, errorable, error)

      val requestDecoderCache: input.Cache = input.createCache()
      val responseEncoderCache: output.Cache = output.createCache()
      val errorResponseEncoderCache: error.Cache = error.createCache()
    }

}
