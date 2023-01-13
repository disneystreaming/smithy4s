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
import smithy4s.{Endpoint, Hints, Schema, ShapeId, StreamingSchema}
import smithy4s.http.CodecAPI
import smithy4s.kinds._

// format: off
/**
 * An interpreter for unary operations in the AWS_QUERY protocol
 */
private[aws] class AwsQueryRPCInterpreter[Alg[_[_, _, _, _, _]], Op[_,_,_,_,_], F[_]](
    service: smithy4s.Service.Aux[Alg, Op],
    endpointPrefix: String,
    awsEnv: AwsEnvironment[F],
    contentType: String,
    codecAPI: CodecAPI
)(implicit F: MonadThrow[F])
    extends PolyFunction5[Op, AwsCall[F, *, *, *, *, *]] {
// format: on

  def apply[I, E, O, SI, SO](
      op: Op[I, E, O, SI, SO]
  ): AwsCall[F, I, E, O, SI, SO] = {
    val (input, endpoint) = service.endpoint(op)
    awsEndpoints(endpoint).toAwsCall(input)
  }

  private def amendEndpoint[I, E, O, SI, SO](
      endpoint: Endpoint[Op, I, E, O, SI, SO]
  ): Endpoint[Op, I, E, O, SI, SO] = {
    new Endpoint[Op, I, E, O, SI, SO] {
      def id: ShapeId = endpoint.id
      def input: Schema[I] =
        endpoint.input.addHints(
          smithy4s.aws.query
            .AwsQueryEnrichment(endpoint.id.name, service.version)
        )
      def output: Schema[O] = endpoint.output
      def streamedInput: StreamingSchema[SI] = endpoint.streamedInput
      def streamedOutput: StreamingSchema[SO] = endpoint.streamedOutput
      def hints: Hints = endpoint.hints
      def wrap(input: I): Op[I, E, O, SI, SO] = endpoint.wrap(input)
    }
  }

  private val signer: AwsSigner[F] = AwsSigner.rpcSigner[F](
    service.id,
    endpointPrefix,
    awsEnv,
    contentType
  )

  private val awsEndpoints =
    new PolyFunction5[
      Endpoint[Op, *, *, *, *, *],
      AwsUnaryEndpoint[F, Op, *, *, *, *, *]
    ] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint[Op, I, E, O, SI, SO]
      ): AwsUnaryEndpoint[F, Op, I, E, O, SI, SO] =
        new AwsUnaryEndpoint(awsEnv, signer, amendEndpoint(endpoint), codecAPI)
    }.unsafeCacheBy(
      service.endpoints.map(Kind5.existential(_)),
      identity
    )

}
