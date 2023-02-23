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

package smithy4s.http4s.internals

import smithy4s.http4s.kernel._
// import smithy4s.http.Metadata
import smithy4s.schema.CachedSchemaCompiler

private[http4s] trait ServerCompilerContext[F[_]] {
  // format: off
  val requestDecoderCompiler: CachedSchemaCompiler[RequestDecoder[F, *]]
  val requestDecoderCache: requestDecoderCompiler.Cache
  val responseEncoderCompiler: CachedSchemaCompiler[ResponseEncoder[F, *]]
  val responseEncoderCache: responseEncoderCompiler.Cache
  // format: on
}

private[http4s] object ServerCompilerContext {

  def makeServer[F[_]](
      input: CachedSchemaCompiler[RequestDecoder[F, *]],
      output: CachedSchemaCompiler[ResponseEncoder[F, *]]
  ): ServerCompilerContext[F] =
    new ServerCompilerContext[F] {
      //format: off
      val requestDecoderCompiler: CachedSchemaCompiler[RequestDecoder[F, *]] = input
      val requestDecoderCache: requestDecoderCompiler.Cache = requestDecoderCompiler.createCache()
      val responseEncoderCompiler: CachedSchemaCompiler[ResponseEncoder[F, *]] = output
      val responseEncoderCache: responseEncoderCompiler.Cache = responseEncoderCompiler.createCache()
    }

}
