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

import _root_.aws.api.{Service => AwsService}
import cats.effect.Async
import cats.effect.Resource
import cats.syntax.all._
import fs2.compression.Compression
import smithy4s.aws.internals.AwsQueryCodecs
import smithy4s.aws.internals._
import smithy4s.http4s.kernel._

object AwsClient {

  def apply[Alg[_[_, _, _, _, _]], F[_]: Async: Compression](
      service: smithy4s.Service[Alg],
      awsEnv: AwsEnvironment[F]
  ): Resource[F, service.Impl[F]] =
    prepare(service)
      .leftWiden[Throwable]
      .map(_.build(awsEnv))
      .liftTo[Resource[F, *]]

  def prepare[Alg[_[_, _, _, _, _]]](
      service: smithy4s.Service[Alg]
  ): Either[AwsClientInitialisationError, AWSInterpreterBuilder[Alg]] =
    for {
      awsService <- service.hints
        .get(AwsService)
        .toRight(AwsClientInitialisationError.NotAws(service.id))
      awsProtocol <- AwsProtocol(service.hints).toRight(
        AwsClientInitialisationError.UnsupportedProtocol(
          serviceId = service.id,
          knownProtocols = AwsProtocol.supportedProtocols
        )
      )
    } yield new AWSInterpreterBuilder(awsProtocol, awsService, service)

  final class AWSInterpreterBuilder[Alg[_[_, _, _, _, _]]](
      awsProtocol: AwsProtocol,
      awsService: AwsService,
      val service: smithy4s.Service[Alg]
  ) {

    private def interpreter[F[_]: Async: Compression](
        awsEnv: AwsEnvironment[F]
    ): service.FunctorInterpreter[F] = {
      val clientCodecs: UnaryClientCodecs.Make[F] = awsProtocol match {
        case AwsProtocol.AWS_EC2_QUERY(_) =>
          AwsEcsQueryCodecs.make[F](version = service.version)

        case AwsProtocol.AWS_JSON_1_0(_) =>
          AwsJsonCodecs.make[F]("application/x-amz-json-1.0")

        case AwsProtocol.AWS_JSON_1_1(_) =>
          AwsJsonCodecs.make[F]("application/x-amz-json-1.1")

        case AwsProtocol.AWS_QUERY(_) =>
          AwsQueryCodecs.make[F](version = service.version)

        case AwsProtocol.AWS_REST_JSON_1(_) =>
          AwsRestJsonCodecs.make[F]("application/json")

        case AwsProtocol.AWS_REST_XML(_) =>
          AwsXmlCodecs.make[F]()
      }
      service.functorInterpreter {
        new service.FunctorEndpointCompiler[F] {
          def apply[I, E, O, SI, SO](
              endpoint: service.Endpoint[I, E, O, SI, SO]
          ): I => F[O] =
            new AwsUnaryEndpoint(
              service.id,
              service.hints,
              awsService,
              awsEnv,
              endpoint.schema,
              clientCodecs
            )
        }
      }
    }

    def build[F[_]: Async: Compression](
        awsEnv: AwsEnvironment[F]
    ): service.Impl[F] =
      service.fromPolyFunction(interpreter[F](awsEnv))

    def buildFull[F[_]: Async: Compression](
        awsEnv: AwsEnvironment[F]
    ): AwsClient[Alg, F] =
      service.fromPolyFunction(
        interpreter[F](awsEnv).andThen(AwsCall.liftEffect[F])
      )
  }

}
