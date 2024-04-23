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
import smithy4s.schema.{Schema, ErrorSchema, StreamingSchema}
import smithy4s.capability.MonadThrowLike
import smithy4s.capability.Zipper
import smithy4s.codecs.Decoder
import org.http4s.Request
import smithy4s.codecs.PayloadError
import smithy4s.xml.internals.XmlStartingPath
import smithy4s.xml.Xml
import smithy4s.Hints
import org.http4s.EntityEncoder
import org.http4s.Entity

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
          val middleware = AwsSigning
            .middleware(awsEnv)
            .andThen(AwsPayloadSignature.signSingleChunk)
            .prepare(service)(endpoint)
          val awsEndpoint = new AwsClientEndpoint[F, I, E, O, SI, SO](awsEnv, awsService, endpoint.schema, middleware)
          (endpoint.streamedInput, endpoint.streamedOutput) match {
            case (Some(_), Some(_)) => ??? // todo support biderectional streaming
            case (Some(ssi), None) =>
              awsEndpoint.upload(
                endpoint.input,
                endpoint.error,
                endpoint.output,
                ssi,
                input
              )
            case (None, Some(sso)) =>
              awsEndpoint.downloader(
                endpoint.input,
                endpoint.error,
                endpoint.output,
                sso,
                input
              )
            case (None, None) => ??? // todo support no streaming
          }
        }

      }

  }

}

final class AwsClientEndpoint[F[_]: Async: Compression, I, E, O, SI, SO](
    awsEnv: AwsEnvironment[F],
    awsService: AwsService,
    operationSchema: OperationSchema[I, E, O, SI, SO],
    middleware: Client[F] => Client[F]
) {
  private val finalClient = (middleware)(awsEnv.httpClient)
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

  def upload(
      si: Schema[I],
      se: Option[ErrorSchema[E]],
      so: Schema[O],
      ssi: StreamingSchema[SI],
      input: I
  ): AwsCall[F, I, E, O, SI, SO] = {
    type InputEncoder = (SI => Byte) => (I, AwsStrictInput[F, SI]) => F[Request[F]]
    // how to carry the http4s response resource over in smithy4s httpresponse
    type OutputDecoder = Response[F] => F[O]
    val inputEncoders = Metadata.AwsEncoder
      .mapK(
        smithy4s.codecs.Encoder.pipeToWriterK(HttpRequest.Writer.metadataWriter[Blob])
      )

    val inputWriter: HttpRequest.Writer[Blob, I] =
      HttpEndpoint.cast(operationSchema).toOption match {
        case Some(httpEndpoint) => {
          val httpInputEncoder =
            HttpRequest.Writer.fromHttpEndpoint[Blob, I](httpEndpoint)
          val requestEncoder = inputEncoders.fromSchema(si)
          httpInputEncoder.combine(requestEncoder)
        }
        case None => inputEncoders.fromSchema(si)
      }

    val httpDecoder = {
      val blobDecoder = Xml.decoders.mapK(
        Decoder
          .of[Blob]
          .liftPolyFunction(
            MonadThrowLike
              .liftEitherK[F, PayloadError]
              .andThen(HttpContractError.fromPayloadErrorK[F])
          )
      )

      val metadataDecoder = Metadata.AwsDecoder
      HttpResponse.Decoder
        .restSchemaCompiler(metadataDecoder, blobDecoder, None)
        .fromSchema(so)
    }

    val httpErrorDecoder = {
      val bodyDecoder = Xml.decoders.mapK(
        Decoder
          .of[Blob]
          .liftPolyFunction(
            MonadThrowLike
              .liftEitherK[F, PayloadError]
              .andThen(HttpContractError.fromPayloadErrorK[F])
          )
      )

      val errorBodyDecoder =
        HttpResponse.Decoder.restSchemaCompiler(Metadata.AwsDecoder, bodyDecoder, None)

      val addErrorStartingPath = (_: Hints).add(XmlStartingPath(List("Response", "Errors", "Error")))
      val discriminatorDecoders =
        Xml.decoders.contramapSchema(Schema.transformHintsLocallyK(addErrorStartingPath))

      def toStrict(blob: Blob): F[(Blob, Blob)] = Async[F].pure((blob, blob))

      HttpResponse.Decoder.forErrorAsThrowable(
        se,
        errorBodyDecoder,
        AwsErrorTypeDecoder.fromResponse(discriminatorDecoders),
        toStrict
      )

    }

    val iEncoder: InputEncoder = { convert =>
      { case (input, strictStreamInput) =>
        baseRequest
          .map { req =>
            smithy4s.http4s.kernel.fromSmithy4sHttpRequest(
              inputWriter.write(req, input)
            )
          }
          .map { req =>
            implicit val w: EntityEncoder[F, AwsStrictInput[F, SI]] =
              EntityEncoder.encodeBy() { ss => Entity(ss.payload.map(convert), Some(ss.length)) }
            req.withEntity(strictStreamInput)
          }
      }
    }

    val oDecoder: OutputDecoder = { response =>
      if (response.status.isSuccess)
        toSmithy4sHttpResponse(response)
          .flatMap(httpDecoder.decode)
      else
        toSmithy4sHttpResponse(response)
          .flatMap(httpErrorDecoder.decode)
          .flatMap { err => Async[F].raiseError[O](err) }
    }

    AwsCall
      .upload[F, I, E, O, SI] { convert => stream =>
        iEncoder(convert).apply(input, stream).flatMap { request =>
          finalClient.run(request).use { resp =>
            oDecoder(resp)
          }
        }

      }
      // to resolve:
      // [error] [aws-http4s]  found   : smithy4s.aws.AwsCall[F,I,E,O,SI,Nothing]
      // [error] [aws-http4s]  required: smithy4s.aws.AwsCall[F,I,E,O,SI,SO]
      .wideDownload[SO]
  }

  def downloader(
      si: Schema[I],
      se: Option[ErrorSchema[E]],
      so: Schema[O],
      sso: StreamingSchema[SO],
      input: I
  ): AwsCall[F, I, E, O, SI, SO] = {
    type InputEncoder = I => F[Request[F]]
    // how to carry the http4s response resource over in smithy4s httpresponse
    type OutputDecoder = (Byte => SO) => Resource[F, Response[F]] => Resource[F, AwsDownloadResult[F, O, SO]]

    val inputEncoders = Metadata.AwsEncoder
      .mapK(
        smithy4s.codecs.Encoder.pipeToWriterK(HttpRequest.Writer.metadataWriter[Blob])
      )

    val inputWriter: HttpRequest.Writer[Blob, I] =
      HttpEndpoint.cast(operationSchema).toOption match {
        case Some(httpEndpoint) => {
          val httpInputEncoder =
            HttpRequest.Writer.fromHttpEndpoint[Blob, I](httpEndpoint)
          val requestEncoder = inputEncoders.fromSchema(si)
          httpInputEncoder.combine(requestEncoder)
        }
        case None => inputEncoders.fromSchema(si)
      }

    val httpDecoder = {
      val zipper = Zipper[Decoder[F, HttpResponse[fs2.Stream[F, Byte]], *]]
      val metadataDecoder = Metadata.AwsDecoder
        .mapK(
          HttpResponse.extractMetadata[F](MonadThrowLike.liftEitherK[F, MetadataError])
        )
        .fromSchema(so)

      val bodyDecoder =
        new Decoder[F, HttpResponse[fs2.Stream[F, Byte]], fs2.Stream[F, Byte]]() {
          override def decode(in: HttpResponse[fs2.Stream[F, Byte]]): F[fs2.Stream[F, Byte]] = Async[F].pure(in.body)
        }

      zipper
        .zipMap(metadataDecoder, bodyDecoder) { case (a, b) => (a, b) }
        .compose[Response[F]](smithy4s.http4s.kernel.toSmithy4sHttpResponseStream(_))
    }

    val httpErrorDecoder = {
      val bodyDecoder = Xml.decoders.mapK(
        Decoder
          .of[Blob]
          .liftPolyFunction(
            MonadThrowLike
              .liftEitherK[F, PayloadError]
              .andThen(HttpContractError.fromPayloadErrorK[F])
          )
      )

      val errorBodyDecoder =
        HttpResponse.Decoder.restSchemaCompiler(Metadata.AwsDecoder, bodyDecoder, None)

      val addErrorStartingPath = (_: Hints).add(XmlStartingPath(List("Response", "Errors", "Error")))
      val discriminatorDecoders =
        Xml.decoders.contramapSchema(Schema.transformHintsLocallyK(addErrorStartingPath))

      def toStrict(blob: Blob): F[(Blob, Blob)] = Async[F].pure((blob, blob))

      HttpResponse.Decoder.forErrorAsThrowable(
        se,
        errorBodyDecoder,
        AwsErrorTypeDecoder.fromResponse(discriminatorDecoders),
        toStrict
      )

    }

    val iEncoder: InputEncoder = { input =>
      baseRequest.map { req =>
        smithy4s.http4s.kernel.fromSmithy4sHttpRequest(
          inputWriter.write(req, input)
        )
      }
    }

    val oDecoder: OutputDecoder = {
      convert =>
        { res_ =>
          res_.evalTap(r => r.bodyText.compile.string.flatMap(b => Async[F].delay(println(b)))).evalMap { response =>
            if (response.status.isSuccess)
              httpDecoder
                .decode(response)
                .map { case (o, stream) =>
                  AwsDownloadResult[F, O, SO](o, stream.map(convert))
                }
            else
              smithy4s.http4s.kernel
                .toSmithy4sHttpResponse(response)
                .flatMap(httpErrorDecoder.decode)
                .flatMap { err => Async[F].raiseError[AwsDownloadResult[F, O, SO]](err) }
          }
        }
    }

    AwsCall
      .download[F, I, E, O, SO] { convert =>
        val run = Resource.eval(iEncoder(input)).flatMap { request =>
          finalClient.run(request)
        }
        oDecoder(convert)(run)
      }
      // to resolve:
      // [error] [aws-http4s]  found   : smithy4s.aws.AwsCall[F,I,E,O,Nothing,SO]
      // [error] [aws-http4s]  required: smithy4s.aws.AwsCall[F,I,E,O,SI,SO]
      .wideUpload[SI]
  }
}
