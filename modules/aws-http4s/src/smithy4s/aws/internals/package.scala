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

package smithy4s.aws

import fs2.compression.Compression
import org.http4s.client.Client
import smithy4s.Endpoint

package object internals {

  private[internals] def applyCompression[F[_]: Compression](
      retainUserEncoding: Boolean = true
  ): Endpoint.Middleware[Client[F]] = {
    // val compression =
    //   smithy4s.http4s.kernel.GzipRequestCompression[F](retainUserEncoding)
    // import smithy4s.codecs.Writer
    // hints.get(smithy.api.RequestCompression) match {
    //   case Some(rc) if rc.encodings.contains("gzip") =>
    //     (encoder: RequestEncoderCompiler[F]) =>
    //       encoder.mapK(Writer.andThenK_(compression))
    //   case _ => identity[RequestEncoderCompiler[F]]
    // }
    ???
  }

}
