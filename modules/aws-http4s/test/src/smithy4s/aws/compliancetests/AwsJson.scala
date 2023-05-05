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

import cats.effect.{IO, Resource}
import org.http4s.client.Client
import org.http4s.HttpApp
import smithy4s.kinds.FunctorAlgebra
import smithy4s.{Service, ShapeTag}
import smithy4s.compliancetests._
import smithy4s.http.HttpMediaType
import smithy4s.schema.Schema

object AwsJson {
  def impl[A](
      protocol: smithy4s.ShapeTag.Companion[A]
  ): ReverseRouter[IO] = new ReverseRouter[IO] {
    type Protocol = A
    val protocolTag: ShapeTag[A] = protocol.getTag

    def expectedResponseType(schema: Schema[_]): HttpMediaType = HttpMediaType(
      "application/json"
    )

    def reverseRoutes[Alg[_[_, _, _, _, _]]](
        app: HttpApp[IO],
        testHost: Option[String] = None
    )(implicit service: Service[Alg]): Resource[IO, FunctorAlgebra[Alg, IO]] = {
      AwsEnvironment
        .default(Client.fromHttpApp(app), AwsRegion.US_EAST_1)
        .flatMap(env => AwsClient(service, env))
    }
  }

}
