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

import smithy4s.http4s.kernel._
import cats.syntax.all._
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.http4s.Method
import _root_.aws.api.{Service => AwsService}
import cats.effect.Concurrent
import cats.effect.Resource

// format: off
private[aws] class AwsUnaryEndpoint[F[_], I, E, O, SI, SO](
  serviceId: ShapeId,
  serviceHints: Hints,
  awsService: AwsService,
  awsEnv: AwsEnvironment[F],
  endpoint: Endpoint.Base[I, E, O, SI, SO],
  makeClientCodecs: UnaryClientCodecs.Make[F],
)(implicit effect: Concurrent[F]) extends (I => F[O]) {
// format: on

  val signingClient = AwsSigningClient(
    serviceId,
    endpoint.id,
    serviceHints,
    endpoint.hints,
    awsEnv
  )

  def apply(input: I): F[O] = {
    Resource
      .eval(inputToRequest(input))
      .flatMap(signingClient.run)
      .use { response =>
        outputFromResponse(response)
      }
  }

  // format: off
  val clientCodecs = makeClientCodecs(endpoint)
  import clientCodecs._

  val endpointPrefix = awsService.endpointPrefix.getOrElse(endpoint.id.name)
  // format: on

  def inputToRequest(input: I): F[Request[F]] = {
    awsEnv.region.map { region =>
      val baseUri: Uri =
        Uri.unsafeFromString(s"https://$endpointPrefix.$region.amazonaws.com/")
      val baseRequest = Request[F](Method.POST, baseUri).withEmptyBody
      inputEncoder.encode(baseRequest, input)
    }
  }

  private def outputFromResponse(response: Response[F]): F[O] =
    if (response.status.isSuccess) outputDecoder.decodeResponse(response)
    else errorDecoder.decodeResponse(response).flatMap(effect.raiseError[O](_))

}
