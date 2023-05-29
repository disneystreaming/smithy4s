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
package internals

import _root_.aws.api.{Service => AwsService}
import org.http4s.client.Client
import smithy4s.http4s.kernel._
import cats.effect.Concurrent

// scalafmt: { align.preset = most, danglingParentheses.preset = false, maxColumn = 240, align.tokens = [{code = ":"}]}

private[aws] class AwsInterpreter[Alg[_[_, _, _, _, _]], F[_]](
    val service:      smithy4s.Service[Alg],
    awsService:       AwsService,
    client:           Client[F],
    makeClientCodecs: UnaryClientCodecs.Make[F],
    awsEnv:           AwsEnvironment[F]
)(implicit effect:    Concurrent[F]) {
// format: on

  val impl: service.Impl[F] = service.impl {
    new service.FunctorEndpointCompiler[F] {
      def apply[I, E, O, SI, SO](endpoint: service.Endpoint[I, E, O, SI, SO]): I => F[O] =
        new AwsUnaryEndpoint(
          service.id,
          service.hints,
          awsService,
          awsEnv,
          endpoint,
          makeClientCodecs
        )
    }
  }

}
