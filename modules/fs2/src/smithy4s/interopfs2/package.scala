package smithy4s

import fs2.Stream
import fs2.Chunk
import smithy4s.capability.MonadThrowLike
import smithy4s.kinds.PolyFunction
import smithy4s.codecs._
import cats.effect.Concurrent
import cats.MonadThrow
import cats.syntax.all._

// scalafmt: {maxColumn = 120}
package object interopfs2 {

  type ByteStreamEncoder[F[_], A] =
    smithy4s.codecs.Writer[Any, Stream[F, Byte], A]
  type ByteStreamDecoder[F[_], A] =
    smithy4s.codecs.Reader[F, Stream[F, Byte], A]

  implicit def monadThrowShim[F[_]: MonadThrow]: MonadThrowLike[F] =
    new MonadThrowLike[F] {
      def pure[A](a: A): F[A] = MonadThrow[F].pure(a)
      def zipMapAll[A](seq: IndexedSeq[F[Any]])(f: IndexedSeq[Any] => A): F[A] =
        seq.toVector.asInstanceOf[Vector[F[Any]]].sequence.map(f)
      def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = MonadThrow[F].flatMap(fa)(f)
      def raiseError[A](e: Throwable): F[A] = MonadThrow[F].raiseError(e)
    }

  object ByteStreamEncoder {

    def fromPayloadWriterK[F[_]]: PolyFunction[PayloadWriter, ByteStreamEncoder[F, *]] =
      Writer.addingTo[Any].andThenK((blob: Blob) => Stream.chunk(Chunk.array(blob.toArray)))

  }

  object ByteStreamDecoder {

    def fromPayloadReaderK[F[_]: Concurrent]: PolyFunction[PayloadReader, ByteStreamDecoder[F, *]] =
      Reader
        .of[Blob]
        .liftPolyFunction(MonadThrowLike.liftEitherK[F, PayloadError])
        .andThen(Reader.in[F].flatComposeK(collectBytes[F]))

    private def collectBytes[F[_]: Concurrent](
        byteStream: fs2.Stream[F, Byte]
    ): F[Blob] = byteStream.chunks.compile
      .to(Chunk)
      .map(_.flatten)
      .map(chunk => Blob(chunk.toArray))

  }

}
