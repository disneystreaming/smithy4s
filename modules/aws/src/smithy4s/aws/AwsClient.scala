/*
 *  Copyright 2021 Disney Streaming
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

import cats.MonadThrow
import cats.effect.Resource
import cats.syntax.all._
import smithy4s.Transformation

import internals.AwsJsonRPCInterpreter

object AwsClient {

  def apply[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _], F[_]: MonadThrow](
      service: smithy4s.Service[Alg, Op],
      awsEnv: AwsEnvironment[F]
  ): Resource[F, AwsClient[Alg, F]] =
    for {
      awsService <- service.hints
        .get(_root_.aws.api.Service)
        .liftTo[Resource[F, *]](
          initError(s"${service.id.show} is not an AWS service")
        )
      endpointPrefix <- awsService.endpointPrefix.liftTo[Resource[F, *]](
        initError(s"No endpoint prefix for $awsService")
      )
      awsProtocol <- AwsProtocol(service.hints).liftTo[Resource[F, *]](
        initError(
          s"AWS protocol used by ${service.id.show} is not yet supported"
        )
      )
    } yield service.transform(
      interpreter(awsProtocol, service, awsEnv, endpointPrefix)
    )

  private def interpreter[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _], F[_]](
      awsProtocol: AwsProtocol,
      service: smithy4s.Service[Alg, Op],
      awsEnv: AwsEnvironment[F],
      endpointPrefix: String
  )(implicit F: MonadThrow[F]): Transformation[Op, AwsCall[F, *, *, *, *, *]] =
    awsProtocol match {
      case AwsProtocol.AWS_JSON_1_0(_, hintMask) =>
        new AwsJsonRPCInterpreter[Alg, Op, F](
          service,
          endpointPrefix,
          awsEnv,
          "application/x-amz-json-1.0",
          hintMask
        )

      case AwsProtocol.AWS_JSON_1_1(_, hintMask) =>
        new AwsJsonRPCInterpreter[Alg, Op, F](
          service,
          endpointPrefix,
          awsEnv,
          "application/x-amz-json-1.1",
          hintMask
        )
    }

  private def initError(msg: String): Throwable = InitialisationError(msg)
  case class InitialisationError(msg: String) extends Throwable(msg)

}
