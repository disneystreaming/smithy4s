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
package aws

import cats.effect.Async
import cats.effect.Resource
import org.http4s.client.Client

package object http4s {

  implicit final class ServiceOps[Alg[_[_, _, _, _, _]]](
      private[this] val service: smithy4s.Service[Alg]
  ) {

    def awsClient[F[_]: Async](
        client: Client[F],
        awsRegion: AwsRegion
    ): Resource[F, AwsClient[Alg, F]] = for {
      env <- AwsEnvironment.default(AwsHttp4sBackend(client), awsRegion)
      awsClient <- AwsClient(service, env)
    } yield awsClient

  }

}
