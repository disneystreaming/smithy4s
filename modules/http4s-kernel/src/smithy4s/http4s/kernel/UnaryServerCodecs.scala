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

import smithy4s.Endpoint
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.Schema
import fs2.Pure

trait UnaryServerCodecs[F[_], I, E, O] {
  val inputDecoder: RequestDecoder[F, I]
  val outputEncoder: ResponseWriter[Pure, F, O]
  def errorEncoder[EE](schema: Schema[EE]): ResponseWriter[Pure, F, EE]
  val errorEncoder: ResponseWriter[Pure, F, E]
}

object UnaryServerCodecs {

  type For[F[_]] = {
    type toKind5[I, E, O, SI, SO] = UnaryServerCodecs[F, I, E, O]
  }

  type Make[F[_]] =
    smithy4s.kinds.PolyFunction5[Endpoint.Base, For[F]#toKind5]

  def make[F[_]](
      input: CachedSchemaCompiler[RequestDecoder[F, *]],
      output: CachedSchemaCompiler[ResponseWriter[Pure, F, *]],
      error: CachedSchemaCompiler[ResponseWriter[Pure, F, *]],
      errorHeaders: List[String]
  ): Make[F] = new Make[F] {
    val requestDecoderCache: input.Cache = input.createCache()
    val responseEncoderCache: output.Cache = output.createCache()
    val errorResponseEncoderCache: error.Cache = error.createCache()

    def apply[I, E, O, SI, SO](
        endpoint: Endpoint.Base[I, E, O, SI, SO]
    ): UnaryServerCodecs[F, I, E, O] = new UnaryServerCodecs[F, I, E, O] {
      val inputDecoder: RequestDecoder[F, I] =
        input.fromSchema(endpoint.input, requestDecoderCache)
      val outputEncoder: ResponseWriter[Pure, F, O] =
        output.fromSchema(endpoint.output, responseEncoderCache)
      val errorEncoder: ResponseWriter[Pure, F, E] =
        ResponseEncoder.forError(
          errorHeaders,
          endpoint.errorable,
          error
        )
      def errorEncoder[EE](schema: Schema[EE]): ResponseWriter[Pure, F, EE] =
        error.fromSchema(schema, errorResponseEncoderCache)
    }
  }

}
