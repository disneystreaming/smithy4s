/*
 *  Copyright 2021-2023 Disney Streaming
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
import cats.effect.MonadCancelThrow
import smithy4s.Endpoint
import smithy4s.Service
import smithy4s.http4s.kernel.GzipRequestCompression

package object internals {

  private[aws] def compressionMiddleware[F[_]: MonadCancelThrow: Compression](
      retainUserEncoding: Boolean = true
  ): Endpoint.Middleware[Client[F]] = new Endpoint.Middleware[Client[F]] {
    def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
        endpoint: service.Endpoint[_, _, _, _, _]
    ): Client[F] => Client[F] =
      endpoint.hints match {
        case smithy.api.RequestCompression.hint(rc)
            if rc.encodings.contains("gzip") =>
          val compress = GzipRequestCompression[F](retainUserEncoding)
          client => Client[F] { request => client.run(compress(request)) }
        case _ => identity[Client[F]]
      }
  }

}
