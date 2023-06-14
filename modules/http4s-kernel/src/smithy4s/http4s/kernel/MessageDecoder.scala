package smithy4s.http4s.kernel

import cats.MonadThrow
import smithy4s.ConstraintError
import smithy4s.capability.Covariant
import org.http4s.Media
import smithy4s.kinds.PolyFunction
import org.http4s.EntityDecoder
import smithy4s.schema.CachedSchemaCompiler
import cats.effect.Concurrent
import smithy4s.kinds.FunctorK
import cats.Applicative
import cats.FlatMap
import cats.syntax.all._
import smithy4s.PartialData
import smithy4s.schema.Schema
import smithy4s.http.HttpRestSchema
import cats.Monad

trait MessageDecoder[F[_], -Message, A] { self =>
  def decode(message: Message): F[A]

  final def compose[M](f: M => Message): MessageDecoder[F, M, A] =
    new MessageDecoder[F, M, A] {
      def decode(m: M): F[A] = self.decode(f(m))
    }

  final def composeF[M, Message2 <: Message](
      f: M => F[Message2]
  )(implicit F: FlatMap[F]): MessageDecoder[F, M, A] =
    new MessageDecoder[F, M, A] {
      def decode(message: M): F[A] = f(message).flatMap(self.decode)
    }

  final def narrow[M2 <: Message]: MessageDecoder[F, M2, A] =
    self.asInstanceOf[MessageDecoder[F, M2, A]]

}

object MessageDecoder {

  implicit def applicativeMessageDecoder[F[_]: Applicative, Message]
      : Applicative[MessageDecoder[F, Message, *]] =
    new Applicative[MessageDecoder[F, Message, *]] {
      def pure[A](a: A): MessageDecoder[F, Message, A] =
        new MessageDecoder[F, Message, A] {
          def decode(message: Message): F[A] = Applicative[F].pure(a)
        }
      def ap[A, B](ff: MessageDecoder[F, Message, A => B])(
          fa: MessageDecoder[F, Message, A]
      ): MessageDecoder[F, Message, B] = new MessageDecoder[F, Message, B] {
        def decode(message: Message): F[B] =
          Applicative[F].ap(ff.decode(message))(fa.decode(message))
      }
    }

  implicit def covariantMessageDecoder[F[_]: MonadThrow, Message]
      : Covariant[MessageDecoder[F, Message, *]] =
    new Covariant[MessageDecoder[F, Message, *]] {
      def map[A, B](
          fa: MessageDecoder[F, Message, A]
      )(f: A => B): MessageDecoder[F, Message, B] =
        new MessageDecoder[F, Message, B] {
          def decode(message: Message): F[B] =
            fa.decode(message).map(f)
        }

      def emap[A, B](fa: MessageDecoder[F, Message, A])(
          f: A => Either[ConstraintError, B]
      ): MessageDecoder[F, Message, B] = new MessageDecoder[F, Message, B] {
        def decode(message: Message): F[B] =
          fa.decode(message).map(f).flatMap(_.liftTo[F])
      }
    }

  def fromEntityDecoder[F[_], A](implicit
      F: MonadThrow[F],
      entityDecoder: EntityDecoder[F, A]
  ): MessageDecoder[F, Media[F], A] = new MessageDecoder[F, Media[F], A] {
    def decode(response: Media[F]): F[A] = response.as[A]
  }

  def fromEntityDecoderK[F[_]: MonadThrow]
      : PolyFunction[EntityDecoder[F, *], MessageDecoder[F, Media[F], *]] =
    new PolyFunction[EntityDecoder[F, *], MessageDecoder[F, Media[F], *]] {
      def apply[A](fa: EntityDecoder[F, A]): MessageDecoder[F, Media[F], A] =
        fromEntityDecoder(MonadThrow[F], fa)
    }

  type CachedCompiler[F[_], Message] =
    CachedSchemaCompiler[MessageDecoder[F, Message, *]]

  /**
    * A compiler for MessageDecoder that encodes the whole data in the body
    * of the request
    */
  def rpcSchemaCompiler[F[_]: Concurrent](
      entityDecoderCompiler: CachedSchemaCompiler[EntityDecoder[F, *]]
  ): CachedCompiler[F, Media[F]] =
    FunctorK[CachedSchemaCompiler].mapK(
      entityDecoderCompiler,
      fromEntityDecoderK[F]
    )

  /**
    * A compiler for MessageDecoder that abides by REST-semantics :
    * fields that are annotated with `httpLabel`, `httpHeader`, `httpQuery`,
    * `httpStatusCode` ... are decoded from the corresponding metadata.
    *
    * The rest is decoded from the body.
    */
  // scalafmt: {maxColumn = 120}
  private[kernel] def restCombinedSchemaCompiler[F[_]: Monad, Message <: Media[F]](
      metadataDecoderCompiler: CachedCompiler[F, Message],
      bodyDecoderCompiler: CachedCompiler[F, Message]
  ): CachedSchemaCompiler[MessageDecoder[F, Message, *]] =
    new CachedSchemaCompiler[MessageDecoder[F, Message, *]] {
      val applicative = applicativeMessageDecoder[F, Message]

      type MetadataCache = metadataDecoderCompiler.Cache
      type BodyCache = bodyDecoderCompiler.Cache
      type Cache = (MetadataCache, BodyCache)
      def createCache(): Cache = {
        val mCache = metadataDecoderCompiler.createCache()
        val bCache = bodyDecoderCompiler.createCache()
        (mCache, bCache)
      }
      def fromSchema[A](schema: Schema[A]) =
        fromSchema(schema, createCache())

      def fromSchema[A](fullSchema: Schema[A], cache: Cache) = {
        HttpRestSchema(fullSchema) match {
          case HttpRestSchema.OnlyMetadata(metadataSchema) =>
            // The data can be fully decoded from the metadata,
            // but we still decoding Unit from the body to drain the message.
            val metadataDecoder =
              metadataDecoderCompiler.fromSchema(metadataSchema, cache._1)
            val bodyDecoder =
              bodyDecoderCompiler.fromSchema(Schema.unit, cache._2)
            (bodyDecoder, metadataDecoder).mapN { case (_, data) => data }
          case HttpRestSchema.OnlyBody(bodySchema) =>
            // The data can be fully decoded from the body
            bodyDecoderCompiler.fromSchema(bodySchema, cache._2)
          case HttpRestSchema.MetadataAndBody(metadataSchema, bodySchema) =>
            val metadataDecoder: MessageDecoder[F, Message, PartialData[A]] =
              metadataDecoderCompiler.fromSchema(metadataSchema, cache._1)
            val bodyDecoder: MessageDecoder[F, Message, PartialData[A]] =
              bodyDecoderCompiler.fromSchema(bodySchema, cache._2)
            (metadataDecoder, bodyDecoder).mapN(
              PartialData.unsafeReconcile(_, _)
            )
          case HttpRestSchema.Empty(value) =>
            applicative.pure(value)
          // format: on
        }
      }
    }

}
