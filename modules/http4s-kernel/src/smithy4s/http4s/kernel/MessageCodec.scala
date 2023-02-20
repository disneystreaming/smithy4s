package smithy4s.http4s.kernel

import smithy4s.http.Metadata
import org.http4s.Request
import org.http4s.Response
import smithy4s.http.CodecAPI
import smithy4s.http.PathParams
import org.typelevel.vault.Key
import cats.MonadThrow
import cats.syntax.all._

trait MessageEncoder[F[_], A] {
  def encodeRequest(a: A): Request[F]
  def encodeResponse(a: A): Response[F]
}

trait MessageDecoder[F[_], A] {
  def decodeRequest(request: Request[F]): F[A]
  def decodeReponse(response: Response[F]): F[A]
}

trait MessageCodec[F[_], A]
    extends MessageEncoder[F, A]
    with MessageDecoder[F, A]

object MessageCodec {

  class TotalMetadataMessageDecoder[F[_]: MonadThrow, A](
      metadataDecoder: Metadata.TotalDecoder[A],
      pathParamsKey: Key[PathParams]
  ) extends MessageDecoder[F, A] {
    def decodeRequest(request: Request[F]): F[A] = {
      // TODO better recovery when the pathParams cannot be retrieved from the vault
      val queryParams =
        request.attributes.lookup(pathParamsKey).getOrElse(Map.empty)
      val metadata = getRequestMetadata(queryParams, request)
      MonadThrow[F].fromEither(metadataDecoder.decode(metadata))
    }

    def decodeReponse(response: Response[F]): F[A] = {
      val metadata = getResponseMetadata(response)
      MonadThrow[F].fromEither(metadataDecoder.decode(metadata))
    }
  }

  private class CodecAPIMessageCodec[F[_], A](
      codecAPI: CodecAPI,
      schema: Schema[A],
      pathParamsKey: Key[PathParams]
  ) extends MessageDecoder[F, A] {

    def decodeRequest(request: Request[F]): F[A] = {
      val pathParams =
        request.attributes.lookup(pathParamsKey).getOrElse(Map.empty)
      val headers = getHeaders(request)
      val queryParams = getQueryParams(request)

    }

    def decodeReponse(response: Response[F]): F[A] = ???

  }

}
