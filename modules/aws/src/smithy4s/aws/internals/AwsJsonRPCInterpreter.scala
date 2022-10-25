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
package internals

import cats.MonadThrow
import smithy4s.Endpoint
import smithy4s.Transformation

// format: off
/**
 * An interpreter for unary operations in the AWS_JSON_1.0/AWS_JSON_1.1 protocol
 */
private[aws] class AwsJsonRPCInterpreter[Alg[_[_, _, _, _, _]], Op[_,_,_,_,_], F[_]](
    service: smithy4s.Service[Alg, Op],
    endpointPrefix: String,
    awsEnv: AwsEnvironment[F],
    contentType: String
)(implicit F: MonadThrow[F])
    extends Transformation[Op, AwsCall[F, *, *, *, *, *]] {
// format: on

  val codecAPI = new json.AwsJsonCodecAPI()

  def apply[I, E, O, SI, SO](
      op: Op[I, E, O, SI, SO]
  ): AwsCall[F, I, E, O, SI, SO] = {
    val (input, endpoint) = service.endpoint(op)
    awsEndpoints(endpoint).toAwsCall(input)
  }

  private val signer: AwsSigner[F] = AwsSigner.rpcSigner[F](
    service.id,
    endpointPrefix,
    awsEnv,
    contentType
  )

  private val awsEndpoints =
    new Transformation[
      Endpoint[Op, *, *, *, *, *],
      AwsUnaryEndpoint[F, Op, *, *, *, *, *]
    ] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint[Op, I, E, O, SI, SO]
      ): AwsUnaryEndpoint[F, Op, I, E, O, SI, SO] =
        new AwsUnaryEndpoint(awsEnv, signer, endpoint, codecAPI)
    }.precompute(service.endpoints.map(smithy4s.Kind5.existential(_)))

}
