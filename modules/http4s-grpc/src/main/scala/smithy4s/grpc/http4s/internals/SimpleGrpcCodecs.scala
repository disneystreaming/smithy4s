package smithy4s
package grpc
package http4s
package internals

import cats.effect.Concurrent
import org.http4s.Uri
import smithy4s.client.UnaryClientCodecs
import smithy4s.server.UnaryServerCodecs
import org.http4s.Request
import org.http4s.Response
import smithy4s.interopcats._
import smithy4s.http4s.kernel._
import smithy4s.Blob
import smithy4s.protobuf.GrpcPayloadCodecCompiler
import org.http4s.Header
import smithy4s.http4s.SimpleProtocolCodecs
import smithy4s.grpc.GrpcResponse

private[grpc] class SimpleGrpcCodecs(
  val grpcPayloadCodecs: GrpcPayloadCodecCompiler
) extends SimpleProtocolCodecs {

  def makeServerCodecs[F[_]: Concurrent]: UnaryServerCodecs.Make[F, Request[F], Response[F]] = {
    val baseHeaders = List[Header.ToRaw](
      GrpcHeaders.GrpcEncoding,
      GrpcHeaders.ContentType
    )

    val baseResponse = GrpcResponse(GrpcStatus.Ok, GrpcHeaders.toSmithy4sHeader(baseHeaders), Blob.empty)
    GrpcUnaryServerCodecs
      .builder[F]
      .withBodyDecoders(grpcPayloadCodecs.decoders)
      .withSuccessBodyEncoders(grpcPayloadCodecs.encoders)
      .withErrorBodyEncoders(grpcPayloadCodecs.encoders)
      .withBaseResponse(_ => Concurrent[F].pure(baseResponse))
      .withRequestTransformation[Request[F]](toGrpcRequest[F](_))
      .withResponseTransformation(r => Concurrent[F].pure(fromGrpcResponse[F](r)))
      .build()
  }

  def makeClientCodecs[F[_]: Concurrent](uri: Uri): UnaryClientCodecs.Make[F, Request[F], Response[F]] = {
    val baseHeaders = List[Header.ToRaw](
      GrpcHeaders.TE,
      GrpcHeaders.GrpcEncoding,
      GrpcHeaders.GrpcAcceptEncoding,
      GrpcHeaders.ContentType,
    )

    val baseRequest = GrpcRequest(toSmithy4sHttpUri(uri, None), GrpcHeaders.toSmithy4sHeader(baseHeaders), Blob.empty)
    GrpcUnaryClientCodecs
      .builder[F]
      .withBaseRequest(endpoint => Concurrent[F].pure(baseRequest.copy(uri = toSmithy4sHttpUri(fromSmithy4sHttpUri(baseRequest.uri, false)  / endpoint.id.name))))
      .withBodyEncoders(grpcPayloadCodecs.encoders)
      .withSuccessBodyDecoders(grpcPayloadCodecs.decoders)
      .withErrorBodyDecoders(grpcPayloadCodecs.decoders)
      .withRequestTransformation(r => Concurrent[F].pure(fromGrpcRequest[F](r, encodePathSegments = false)))
      .withResponseTransformation[Response[F]](toGrpcResponse[F](_))
      .build()
  }
}