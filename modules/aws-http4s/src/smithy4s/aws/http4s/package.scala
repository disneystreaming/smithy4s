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

import cats.effect.Temporal
import cats.effect.Resource
import org.http4s.client.Client

package object http4s {

  implicit final class ServiceOps[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]](
      private[this] val serviceProvider: smithy4s.Service.Provider[Alg, Op]
  ) {

    def awsClient[F[_]: Temporal](
        client: Client[F],
        awsRegion: AwsRegion
    ): Resource[F, AwsClient[Alg, F]] = for {
      env <- AwsEnvironment.default(AwsHttp4sBackend(client), awsRegion)
      awsClient <- AwsClient(serviceProvider.service, env)
    } yield awsClient

  }

}
