package smithy4s.http4s.kernel

import org.http4s.Request
import org.http4s.Response
import cats.MonadThrow
import org.http4s.EntityDecoder
import smithy4s.schema._
import smithy4s.PartialData
import cats.effect.Concurrent
import smithy4s.http.HttpRestSchema
import smithy4s.http.Metadata
import cats.syntax.all._

trait MessageDecoder[F[_], A]
    extends RequestDecoder[F, A]
    with ResponseDecoder[F, A]

object MessageDecoder {

  def fromEntityDecoder[F[_], A](implicit
      F: MonadThrow[F],
      entityDecoder: EntityDecoder[F, A]
  ): MessageDecoder[F, A] = new MessageDecoder[F, A] {
    def decodeRequest(request: Request[F]): F[A] = request.as[A]

    def decodeResponse(response: Response[F]): F[A] = response.as[A]
  }

  /**
    * Creates a MessageDecoder that decodes an HTTP message by looking at the
    * metadata.
    *
    * NB: This decoder assumes that incoming requests have been enriched with pre-extracted
    * path-parameters in the vault.
    */
  def fromMetadataDecoder[F[_]: Concurrent, A](
      metadataDecoder: Metadata.Decoder[A],
      drainMessage: Boolean
  ): MessageDecoder[F, A] = new MessageDecoder[F, A] {
    def decodeRequest(request: Request[F]): F[A] = {
      // TODO better recovery when the pathParams cannot be retrieved from the vault
      val queryParams =
        request.attributes.lookup(pathParamsKey).getOrElse(Map.empty)
      val metadata = getRequestMetadata(queryParams, request)
      val drain = request.body.compile.drain.whenA(drainMessage)
      val decode = MonadThrow[F].fromEither(metadataDecoder.decode(metadata))
      decode <* drain
    }

    def decodeResponse(response: Response[F]): F[A] = {
      val metadata = getResponseMetadata(response)
      val decode = MonadThrow[F].fromEither(metadataDecoder.decode(metadata))
      val drain = response.body.compile.drain.whenA(drainMessage)
      decode <* drain
    }
  }

  def rpcSchemaCompiler[F[_]](
      entityDecoderCompiler: CachedSchemaCompiler[EntityDecoder[F, *]]
  )(implicit F: MonadThrow[F]): CachedSchemaCompiler[MessageDecoder[F, *]] =
    new CachedSchemaCompiler[MessageDecoder[F, *]] {
      type Cache = entityDecoderCompiler.Cache
      def createCache(): Cache =
        entityDecoderCompiler.createCache()

      def fromSchema[A](schema: Schema[A], cache: Cache): MessageDecoder[F, A] =
        fromEntityDecoder(F, entityDecoderCompiler.fromSchema(schema, cache))
      def fromSchema[A](schema: Schema[A]): MessageDecoder[F, A] =
        fromEntityDecoder(F, entityDecoderCompiler.fromSchema(schema))
    }

  /**
    * A compiler for MessageDecoder that abides by REST-semantics :
    * fields that are annotated with `httpLabel`, `httpHeader`, `httpQuery`,
    * `httpStatusCode` ... are decoded from the corresponding metadata.
    *
    * The rest is decoded from the body.
    */
  def restSchemaCompiler[F[_]](
      entityDecoderCompiler: CachedSchemaCompiler[EntityDecoder[F, *]]
  )(implicit
      F: Concurrent[F]
  ): CachedSchemaCompiler[MessageDecoder[F, *]] =
    new CachedSchemaCompiler[MessageDecoder[F, *]] {
      type MetadataCache = Metadata.Decoder.Cache
      type EntityCache = entityDecoderCompiler.Cache
      type Cache = (EntityCache, MetadataCache)
      def createCache(): Cache = {
        val eCache = entityDecoderCompiler.createCache()
        val mCache = Metadata.Decoder.createCache()
        (eCache, mCache)
      }
      def fromSchema[A](schema: Schema[A]): MessageDecoder[F, A] =
        fromSchema(schema, createCache())

      def fromSchema[A](
          fullSchema: Schema[A],
          cache: Cache
      ): MessageDecoder[F, A] = {
        HttpRestSchema(fullSchema) match {
          case HttpRestSchema.OnlyMetadata(metadataSchema) =>
            // The data can be fully decoded from the metadata.
            val metadataDecoder =
              Metadata.Decoder.fromSchema(metadataSchema, cache._2)
            MessageDecoder.fromMetadataDecoder(
              metadataDecoder,
              drainMessage = true
            )
          case HttpRestSchema.OnlyBody(bodySchema) =>
            // The data can be fully decoded from the body
            implicit val bodyDecoder: EntityDecoder[F, A] =
              entityDecoderCompiler.fromSchema(bodySchema, cache._1)
            MessageDecoder.fromEntityDecoder(F, bodyDecoder)
          case HttpRestSchema.MetadataAndBody(metadataSchema, bodySchema) =>
            val metadataDecoder =
              Metadata.Decoder.fromSchema(metadataSchema, cache._2)
            val metadataMessageDecoder =
              MessageDecoder.fromMetadataDecoder[F, PartialData[A]](
                metadataDecoder,
                drainMessage = false
              )
            implicit val bodyDecoder: EntityDecoder[F, PartialData[A]] =
              entityDecoderCompiler.fromSchema(bodySchema, cache._1)

            // format: off
            new MessageDecoder[F, A] {
              def decodeRequest(request: Request[F]): F[A] = for {
                metadataPartial <- metadataMessageDecoder.decodeRequest(request)
                bodyPartial <- request.as[PartialData[A]]
              } yield PartialData.unsafeReconcile(metadataPartial, bodyPartial)

              def decodeResponse(response: Response[F]): F[A] = for {
                metadataPartial <- metadataMessageDecoder.decodeResponse(response)
                bodyPartial <- response.as[PartialData[A]]
              } yield PartialData.unsafeReconcile(metadataPartial, bodyPartial)
            }
            //format: on
        }
      }
    }

}
