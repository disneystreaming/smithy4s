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

import smithy4s.schema.{Schema, CachedSchemaCompiler}

trait HttpUnaryServerCodecs[F[_], Body, I, E, O] {
  val inputDecoder: HttpRequest.Decoder[F, Body, I]
  val outputEncoder: HttpResponse.Encoder[Body, O]
  val errorEncoder: HttpResponse.Encoder[Body, E]
  def errorEncoder[EE](schema: Schema[EE]): HttpResponse.Encoder[Body, EE]
}

// scalafmt: {maxColumn = 120}
object HttpUnaryServerCodecs {

  type For[F[_], Body] = {
    type toKind5[I, E, O, SI, SO] = HttpUnaryServerCodecs[F, Body, I, E, O]
  }

  type Make[F[_], Body] =
    smithy4s.kinds.PolyFunction5[Endpoint.Base, For[F, Body]#toKind5]

  def make[F[_], Body](
      input: CachedSchemaCompiler[HttpRequest.Decoder[F, Body, *]],
      output: CachedSchemaCompiler[HttpResponse.Encoder[Body, *]],
      error: CachedSchemaCompiler[HttpResponse.Encoder[Body, *]],
      errorHeaders: List[String]
  ): Make[F, Body] = new Make[F, Body] {
    val requestDecoderCache: input.Cache = input.createCache()
    val responseEncoderCache: output.Cache = output.createCache()
    val errorResponseEncoderCache: error.Cache = error.createCache()

    def apply[I, E, O, SI, SO](
        endpoint: Endpoint.Base[I, E, O, SI, SO]
    ): HttpUnaryServerCodecs[F, Body, I, E, O] =
      new HttpUnaryServerCodecs[F, Body, I, E, O] {
        val inputDecoder: HttpRequest.Decoder[F, Body, I] =
          input.fromSchema(endpoint.input, requestDecoderCache)
        val outputEncoder: HttpResponse.Encoder[Body, O] =
          output.fromSchema(endpoint.output, responseEncoderCache)
        val errorEncoder: HttpResponse.Encoder[Body, E] =
          HttpResponse.Encoder.forError(errorHeaders, endpoint.errorable, error)
        def errorEncoder[EE](schema: Schema[EE]): HttpResponse.Encoder[Body, EE] =
          error.fromSchema(schema, errorResponseEncoderCache)
      }
  }

}
