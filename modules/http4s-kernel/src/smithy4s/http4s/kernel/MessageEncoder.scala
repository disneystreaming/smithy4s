package smithy4s.http4s.kernel

import org.http4s.Request
import org.http4s.Response
import org.http4s.EntityEncoder
import org.http4s.Status
import smithy4s.schema._
import smithy4s.PartialData
import cats.effect.Concurrent
import smithy4s.http.HttpRestSchema
import smithy4s.http.Metadata

trait MessageEncoder[F[_], A]
    extends RequestEncoder[F, A]
    with ResponseEncoder[F, A] { self =>
  def contramap[B](f: B => A): MessageEncoder[F, B] = new MessageEncoder[F, B] {
    def addToRequest(request: Request[F], b: B): Request[F] =
      self.addToRequest(request, f(b))
    def addToResponse(response: Response[F], b: B): Response[F] =
      self.addToResponse(response, f(b))
  }
}

object MessageEncoder {

  def combine[F[_], A](
      left: MessageEncoder[F, A],
      right: MessageEncoder[F, A]
  ): MessageEncoder[F, A] = new MessageEncoder[F, A] {
    def addToRequest(request: Request[F], a: A): Request[F] =
      right.addToRequest(left.addToRequest(request, a), a)
    def addToResponse(response: Response[F], a: A): Response[F] =
      right.addToResponse(left.addToResponse(response, a), a)
  }

  def fromMetadataEncoder[F[_]: Concurrent, A](
      metadataEncoder: Metadata.Encoder[A]
  ): MessageEncoder[F, A] = new MessageEncoder[F, A] {
    def addToRequest(request: Request[F], a: A): Request[F] = {
      val metadata = metadataEncoder.encode(a)
      val uri = request.uri
        .withMultiValueQueryParams(metadata.query)
      val headers = toHeaders(metadata.headers)
      request.withUri(uri).withHeaders(request.headers ++ headers)
    }

    def addToResponse(response: Response[F], a: A): Response[F] = {
      val metadata = metadataEncoder.encode(a)
      val headers = toHeaders(metadata.headers)
      val status = metadata.statusCode
        .flatMap(Status.fromInt(_).toOption)
        .getOrElse(response.status)
      response.withHeaders(response.headers ++ headers).withStatus(status)
    }
  }

  def fromEntityEncoder[F[_]: Concurrent, A](implicit
      entityEncoder: EntityEncoder[F, A]
  ): MessageEncoder[F, A] = new MessageEncoder[F, A] {
    def addToRequest(request: Request[F], a: A): Request[F] = {
      request.withEntity(a)
    }

    def addToResponse(response: Response[F], a: A): Response[F] = {
      response.withEntity(a)
    }
  }

  /**
    * A compiler for MessageEncoder that abides by REST-semantics :
    * fields that are annotated with `httpLabel`, `httpHeader`, `httpQuery`,
    * `httpStatusCode` ... are encoded as the corresponding metadata.
    *
    * The rest is used to formulate the body of the message.
    */
  def restSchemaCompiler[F[_]](
      entityEncoderCompiler: CachedSchemaCompiler[EntityEncoder[F, *]]
  )(implicit
      F: Concurrent[F]
  ): CachedSchemaCompiler[MessageEncoder[F, *]] =
    new CachedSchemaCompiler[MessageEncoder[F, *]] {
      type MetadataCache = Metadata.Encoder.Cache
      type EntityCache = entityEncoderCompiler.Cache
      type Cache = (EntityCache, MetadataCache)
      def createCache(): Cache = {
        val eCache = entityEncoderCompiler.createCache()
        val mCache = Metadata.Encoder.createCache()
        (eCache, mCache)
      }
      def fromSchema[A](schema: Schema[A]): MessageEncoder[F, A] =
        fromSchema(schema, createCache())

      def fromSchema[A](
          fullSchema: Schema[A],
          cache: Cache
      ): MessageEncoder[F, A] = {
        HttpRestSchema(fullSchema) match {
          case HttpRestSchema.OnlyMetadata(metadataSchema) =>
            // The data can be fully decoded from the metadata.
            val metadataEncoder =
              Metadata.Encoder.fromSchema(metadataSchema, cache._2)
            MessageEncoder.fromMetadataEncoder(metadataEncoder)
          case HttpRestSchema.OnlyBody(bodySchema) =>
            // The data can be fully decoded from the body
            implicit val bodyDecoder: EntityEncoder[F, A] =
              entityEncoderCompiler.fromSchema(bodySchema, cache._1)
            MessageEncoder.fromEntityEncoder(F, bodyDecoder)
          case HttpRestSchema.MetadataAndBody(metadataSchema, bodySchema) =>
            val metadataEncoder =
              Metadata.Encoder.fromSchema(metadataSchema, cache._2)
            val metadataMessageEncoder =
              MessageEncoder
                .fromMetadataEncoder(metadataEncoder)
                .contramap[A](PartialData.Total(_))
            implicit val bodyEncoder: EntityEncoder[F, A] =
              entityEncoderCompiler
                .fromSchema(bodySchema, cache._1)
                .contramap[A](PartialData.Total(_))
            val bodyMessageEncoder =
              MessageEncoder
                .fromEntityEncoder(F, bodyEncoder)
            MessageEncoder.combine(metadataMessageEncoder, bodyMessageEncoder)
          // format: on
        }
      }
    }

}
