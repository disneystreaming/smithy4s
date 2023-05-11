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
import smithy4s.http._
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.http4s.Method
import _root_.aws.api.{Service => AwsService}
import cats.effect.Concurrent
import cats.effect.Resource

// format: off
private[aws] class AwsUnaryEndpoint[F[_], Op[_, _, _, _, _], I, E, O, SI, SO](
  serviceId: ShapeId,
  serviceHints: Hints,
  awsService: AwsService,
  awsEnv: AwsEnvironment[F],
  endpoint: Endpoint[Op, I, E, O, SI, SO],
  clientCodecs: UnaryClientCodecs[F],
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
  val inputEncoder: RequestEncoder[F, I] = {
    // Some AWS protocols abide by REST semantics, some don't
    HttpEndpoint.unapply(endpoint) match {
      case Some(httpEndpoint) => {
        val httpEndpointEncoder = MessageEncoder.fromHttpEndpoint(httpEndpoint)
        val codecsEncoder = clientCodecs.inputEncoder(endpoint.input)
        RequestEncoder.combine(httpEndpointEncoder, codecsEncoder)
      }
      case None => clientCodecs.inputEncoder(endpoint.input)
    }
  }
  val outputDecoder: ResponseDecoder[F, O] = clientCodecs.outputDecoder(endpoint.output)
  val errorDecoder: ResponseDecoder[F, Throwable] = clientCodecs.errorDecoder(endpoint.errorable)
  val endpointPrefix = awsService.endpointPrefix.getOrElse(endpoint.id.name)
  // format: on

  def inputToRequest(input: I): F[Request[F]] = {
    awsEnv.region.map { region =>
      val baseUri: Uri =
        Uri.unsafeFromString(s"https://$endpointPrefix.$region.amazonaws.com/")
      val baseRequest = Request[F](Method.POST, baseUri).withEmptyBody
      inputEncoder.addToRequest(baseRequest, input)
    }
  }

  private def outputFromResponse(response: Response[F]): F[O] =
    if (response.status.isSuccess) outputDecoder.decodeResponse(response)
    else errorDecoder.decodeResponse(response).flatMap(effect.raiseError[O](_))

}
