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

import cats.effect.Concurrent
import cats.effect.Resource
import cats.syntax.all._
import internals.AwsJsonRPCInterpreter
import internals.AwsQueryRPCInterpreter

object AwsClient {

  def apply[Alg[_[_, _, _, _, _]], F[_]: Concurrent](
      service: smithy4s.Service[Alg],
      awsEnv: AwsEnvironment[F]
  ): Resource[F, AwsClient[Alg, F]] =
    prepare(service)
      .leftWiden[Throwable]
      .map(_.build(awsEnv))
      .liftTo[Resource[F, *]]

  def prepare[Alg[_[_, _, _, _, _]]](
      service: smithy4s.Service[Alg]
  ): Either[AwsClientInitialisationError, AWSInterpreterBuilder[Alg]] =
    for {
      awsService <- service.hints
        .get(_root_.aws.api.Service)
        .toRight(AwsClientInitialisationError.NotAws(service.id))
      endpointPrefix <- awsService.endpointPrefix.toRight(
        AwsClientInitialisationError.NoEndpointPrefix(awsService)
      )
      awsProtocol <- AwsProtocol(service.hints).toRight(
        AwsClientInitialisationError.UnsupportedProtocol(
          serviceId = service.id,
          knownProtocols = AwsProtocol.supportedProtocols
        )
      )
    } yield new AWSInterpreterBuilder(awsProtocol, service, endpointPrefix)

  final class AWSInterpreterBuilder[Alg[_[_, _, _, _, _]]](
      awsProtocol: AwsProtocol,
      service: smithy4s.Service[Alg],
      endpointPrefix: String
  ) {

    private def interpreter[F[_]: Concurrent](
        awsEnv: AwsEnvironment[F]
    ): service.Interpreter[AwsCall[F, *, *, *, *, *]] =
      awsProtocol match {
        case AwsProtocol.AWS_JSON_1_0(_) =>
          new AwsJsonRPCInterpreter[Alg, service.Operation, F](
            service,
            endpointPrefix,
            awsEnv,
            "application/x-amz-json-1.0",
            new json.AwsJsonCodecAPI()
          )

        case AwsProtocol.AWS_JSON_1_1(_) =>
          new AwsJsonRPCInterpreter[Alg, service.Operation, F](
            service,
            endpointPrefix,
            awsEnv,
            "application/x-amz-json-1.1",
            new json.AwsJsonCodecAPI()
          )
        case AwsProtocol.AWS_QUERY(_) =>
          new AwsQueryRPCInterpreter[Alg, service.Operation, F](
            service,
            endpointPrefix,
            awsEnv,
            "application/x-www-form-urlencoded"
          )
      }

    def build[F[_]: Concurrent](
        awsEnv: AwsEnvironment[F]
    ): AwsClient[Alg, F] = service.fromPolyFunction(interpreter(awsEnv))
  }

}
