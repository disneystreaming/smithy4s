/*
 *  Copyright 2021-2024 Disney Streaming
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
import org.http4s.Response
import org.http4s.client.Client
import smithy4s.Blob
import smithy4s.aws.internals._
import smithy4s.http._
import smithy4s.client.UnaryClientCompiler
import smithy4s.schema.OperationSchema
import smithy4s.http4s.kernel._
import smithy4s.interopcats._
import smithy4s.http.HttpMethod
import smithy4s.schema.{Schema, StreamingSchema}
import smithy4s.capability.MonadThrowLike

// scalafmt: { maxColumn = 120 }
object AwsClient {

  def apply[Alg[_[_, _, _, _, _]], F[_]: Async: Compression](
      service: smithy4s.Service[Alg],
      awsEnv: AwsEnvironment[F]
  ): Resource[F, service.Impl[F]] =
    prepare(service)
      .leftWiden[Throwable]
      .map(_.build(awsEnv))
      .liftTo[Resource[F, *]]

  def streamingClient[Alg[_[_, _, _, _, _]], F[_]: Async: Compression](
      service: smithy4s.Service[Alg],
      awsEnv: AwsEnvironment[F]
  ): Resource[F, AwsClient[Alg, F]] =
    prepare(service)
      .leftWiden[Throwable]
      .map(_.buildFull(awsEnv))
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

    // private def streamingCompiler[F[_]: Async: Compression](
    //     awsEnv: AwsEnvironment[F]
    // ): PolyFunction5[service.Endpoint, AwsCall[F, *, *, *, *, *]] = {
    //   new PolyFunction5[service.Endpoint, AwsCall[F, *, *, *, *, *]] {
    //     override def apply[I, E, O, SI, SO](
    //         fa: service.Endpoint[I, E, O, SI, SO]
    //     ): AwsCall[F, I, E, O, SI, SO] = {
    //       service.
    //     }
    //   }
    // }

    private def compiler[F[_]: Async: Compression](
        awsEnv: AwsEnvironment[F]
    ): service.FunctorEndpointCompiler[F] = {

      def baseRequest(endpoint: OperationSchema[_, _, _, _, _]): F[HttpRequest[Blob]] = {
        awsEnv.region.map { region =>
          val endpointPrefix = awsService.endpointPrefix.getOrElse(endpoint.id.name)
          val baseUri = HttpUri(
            scheme = HttpUriScheme.Https,
            host = Some(s"$endpointPrefix.$region.amazonaws.com"),
            port = None,
            path = IndexedSeq.empty,
            queryParams = Map.empty,
            pathParams = None
          )
          // Uri.unsafeFromString(s"https://$endpointPrefix.$region.amazonaws.com/")
          HttpRequest(HttpMethod.POST, baseUri, Map.empty, Blob.empty)
        }
      }

      val clientCodecsBuilder: HttpUnaryClientCodecs.Builder[F, HttpRequest[Blob], HttpResponse[Blob]] =
        awsProtocol match {
          case AwsProtocol.AWS_EC2_QUERY(_) =>
            AwsEcsQueryCodecs.make[F](version = service.version)

          case AwsProtocol.AWS_JSON_1_0(_) =>
            AwsJsonCodecs.make[F]("application/x-amz-json-1.0")

          case AwsProtocol.AWS_JSON_1_1(_) =>
            AwsJsonCodecs.make[F]("application/x-amz-json-1.1")

          case AwsProtocol.AWS_QUERY(_) =>
            // TODO retain user encoding when applying compression
            AwsQueryCodecs.make[F](version = service.version)

          case AwsProtocol.AWS_REST_JSON_1(_) =>
            AwsRestJsonCodecs.make[F]("application/json")

          case AwsProtocol.AWS_REST_XML(_) =>
            AwsRestXmlCodecs.make[F]()
        }

      val compression = awsProtocol match {
        case AwsProtocol.AWS_EC2_QUERY(_) => compressionMiddleware[F](false)
        case AwsProtocol.AWS_QUERY(_)     => compressionMiddleware[F](false)
        case _                            => compressionMiddleware[F](true)
      }

      val clientCodecs = clientCodecsBuilder
        .withRequestTransformation(fromSmithy4sHttpRequest[F](_).pure[F])
        .withResponseTransformation[Response[F]](toSmithy4sHttpResponse[F](_))
        .withBaseRequest(baseRequest)
        .build()

      val middleware =
        AwsSigning.middleware(awsEnv).andThen(compression).andThen(Md5CheckSum.middleware[F])

      UnaryClientCompiler(
        service,
        awsEnv.httpClient,
        (client: Client[F]) => Http4sToSmithy4sClient(client),
        clientCodecs,
        middleware,
        (response: Response[F]) => response.status.isSuccess
      )
    }

    def build[F[_]: Async: Compression](
        awsEnv: AwsEnvironment[F]
    ): service.Impl[F] =
      service.impl(compiler[F](awsEnv))

    // TODO : uncomment below when we start supporting streaming.

    def buildFull[F[_]: Async: Compression](
        awsEnv: AwsEnvironment[F]
    ): AwsClient[Alg, F] = {
      service.fromPolyFunction(interpreter[F](awsEnv))
    }

    def interpreter[F[_]: Async: Compression](
        awsEnv: AwsEnvironment[F]
    ): service.Interpreter[AwsCall[F, *, *, *, *, *]] =
      new service.Interpreter[AwsCall[F, *, *, *, *, *]] {
        override def apply[I, E, O, SI, SO](
            operation: service.Operation[I, E, O, SI, SO]
        ): AwsCall[F, I, E, O, SI, SO] = {
          val input = service.input(operation)
          val endpoint = service.endpoint(operation)
          val awsEndpoint = new AwsClientEndpoint[F, I, E, O, SI, SO](awsEnv, awsService, endpoint.schema)
          (endpoint.streamedInput, endpoint.streamedOutput) match {
            case (Some(_), Some(_)) => ??? // todo support biderectional streaming
            case (Some(_), None)    => ??? // todo support upload streaming
            case (None, Some(sso)) =>
              awsEndpoint.downloader(endpoint.input, endpoint.output, sso)(input)
            case (None, None) => ??? // todo support no streaming
          }
        }

      }

  }

}

final class AwsClientEndpoint[F[_]: Async: Compression, I, E, O, SI, SO](
    awsEnv: AwsEnvironment[F],
    awsService: AwsService,
    operationSchema: OperationSchema[I, E, O, SI, SO]
) {
  val baseRequest: F[HttpRequest[Blob]] = {
    awsEnv.region.map { region =>
      val endpointPrefix = awsService.endpointPrefix.getOrElse(operationSchema.id.name)
      val baseUri = HttpUri(
        scheme = HttpUriScheme.Https,
        host = Some(s"$endpointPrefix.$region.amazonaws.com"),
        port = None,
        path = IndexedSeq.empty,
        queryParams = Map.empty,
        pathParams = None
      )
      // Uri.unsafeFromString(s"https://$endpointPrefix.$region.amazonaws.com/")
      HttpRequest(HttpMethod.POST, baseUri, Map.empty, Blob.empty)
    }
  }

  def downloader(
      si: Schema[I],
      so: Schema[O],
      sso: StreamingSchema[SO]
  )(input: I): AwsCall[F, I, E, O, SI, SO] = {
    type InputEncoder = I => F[HttpRequest[Blob]]
    type OutputDecoder = HttpResponse[fs2.Stream[F, Blob]] => Resource[F, (O, fs2.Stream[F, SO])]

    def httpEncoder(req: HttpRequest[Blob]) = Metadata.AwsEncoder
      .mapK(
        smithy4s.codecs.Encoder.pipeToWriterK(HttpRequest.Writer.metadataWriter[Blob])
      )
      .fromSchema(si)
      .toEncoder(req)

    val httpDecoder = Metadata.AwsDecoder
      .mapK(
        HttpResponse.extractMetadata[F](MonadThrowLike.liftEitherK[F, MetadataError])
      )
      .fromSchema(so)

    val iEncoder: InputEncoder = { input =>
      baseRequest.map { req =>
        val encoder = httpEncoder(req)
        encoder.encode(input)
      }
    }

    val oDecoder: OutputDecoder = { res =>
      httpDecoder.decode(res)
      ???
    }

    ???
  }
}
