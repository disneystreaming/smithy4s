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

import cats.syntax.all._
import org.http4s.Request
import org.http4s.Response
import _root_.aws.api.{Service => AwsService}
import smithy4s.client.UnaryClientCodecs
import cats.effect.Async
import cats.effect.Resource

// format: off
private[aws] class AwsUnaryEndpoint[F[_], I, E, O, SI, SO](
  serviceId: ShapeId,
  serviceHints: Hints,
  awsService: AwsService,
  awsEnv: AwsEnvironment[F],
  endpoint: Endpoint.Base[I, E, O, SI, SO],
  makeClientCodecs: UnaryClientCodecs.Make[F, Request[F], Response[F]],
)(implicit effect: Async[F]) extends (I => F[O]) {
// format: on

  val signingClient = AwsSigningClient(
    serviceId,
    endpoint.id,
    serviceHints,
    endpoint.hints,
    awsEnv
  )

  private val withCheckSumClient = Md5CheckSumClient[F](endpoint.hints)

  private val transformedClient = withCheckSumClient(signingClient)

  def apply(input: I): F[O] = {
    Resource
      .eval(inputToRequest(input))
      .flatMap(transformedClient.run)
      .use { response =>
        outputFromResponse(response)
      }
  }

  // format: off
  val clientCodecs = makeClientCodecs(endpoint)
  import clientCodecs._
  // format: on

  def inputToRequest(input: I): F[Request[F]] = {
    clientCodecs.inputEncoder(input)
  }

  private def outputFromResponse(response: Response[F]): F[O] = {
    if (response.status.isSuccess) outputDecoder(response)
    else errorDecoder(response).flatMap(effect.raiseError[O](_))
  }

}
