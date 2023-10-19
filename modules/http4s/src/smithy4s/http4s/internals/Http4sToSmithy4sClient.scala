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

package smithy4s.http4s.internals

import org.http4s.client.Client
import org.http4s.{Request, Response}
import smithy4s.client.UnaryLowLevelClient
import cats.effect.MonadCancelThrow

private[http4s] object Http4sToSmithy4sClient {
  def apply[F[_]: MonadCancelThrow](
      client: Client[F]
  ): UnaryLowLevelClient[F, Request[F], Response[F]] =
    new UnaryLowLevelClient[F, Request[F], Response[F]] {
      def run[Output](request: Request[F])(cb: Response[F] => F[Output]) =
        client.run(request).use(cb)
    }
}
