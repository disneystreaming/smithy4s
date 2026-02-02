package smithy4s.http4s.grpc.internals

import smithy4s.http4s.SimpleProtocolCodecs
import cats.effect.Concurrent
import org.http4s.Uri
import smithy4s.client.UnaryClientCodecs
import smithy4s.server.UnaryServerCodecs
import org.http4s.Request
import org.http4s.Response
import smithy4s.http4s.grpc.GrpcUnaryClientCodecs
import smithy4s.interopcats._
import smithy4s.http4s.kernel._
import smithy4s.Blob
import smithy4s.http.HttpMethod
import smithy4s.http.HttpRequest
import smithy4s.protobuf.GrpcPayloadCodecCompiler
import main.scala.smithy4s.http4s.grpc.GrpcUnaryServerCodecs
import smithy4s.http.HttpResponse
import org.http4s.HttpVersion

private[grpc] class SimpleGrpcCodecs(
  val grpcPayloadCodecs: GrpcPayloadCodecCompiler
) extends SimpleProtocolCodecs {
  def makeServerCodecs[F[_]: Concurrent]: UnaryServerCodecs.Make[F, Request[F], Response[F]] = {
    val baseResponse = HttpResponse(200, Map.empty, Blob.empty)
    GrpcUnaryServerCodecs
      .builder[F]
      .withBodyDecoders(grpcPayloadCodecs.decoders)
      .withSuccessBodyEncoders(grpcPayloadCodecs.encoders)
      .withErrorBodyEncoders(grpcPayloadCodecs.encoders)
      .withBaseResponse(_ => Concurrent[F].pure(baseResponse))
      .withRequestTransformation[Request[F]](toSmithy4sHttpRequest[F](_))
      .withResponseTransformation(r => Concurrent[F].pure(fromSmithy4sHttpResponse[F](r)/*.withHttpVersion(HttpVersion.`HTTP/2`)*/))
      .build()
  }


  def makeClientCodecs[F[_]: Concurrent](uri: Uri): UnaryClientCodecs.Make[F, Request[F], Response[F]] = {
    val baseRequest = HttpRequest(HttpMethod.POST, toSmithy4sHttpUri(uri, None), Map.empty, Blob.empty)
    GrpcUnaryClientCodecs
      .builder[F]
      .withBaseRequest(_ => Concurrent[F].pure(baseRequest))
      .withBodyEncoders(grpcPayloadCodecs.encoders)
      .withSuccessBodyDecoders(grpcPayloadCodecs.decoders)
      .withRequestTransformation(r => Concurrent[F].pure(fromSmithy4sHttpRequest[F](r, encodePathSegments = false)/*.withHttpVersion(HttpVersion.`HTTP/2`)*/))
      .withResponseTransformation[Response[F]](toSmithy4sHttpResponse[F](_))
      .build()
  }
}