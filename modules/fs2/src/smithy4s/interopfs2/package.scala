package smithy4s

import _root_.fs2.Stream

package object interopfs2 {

  type ByteStreamEncoder[F[_], A] =
    smithy4s.codecs.Writer[Any, Stream[F, Byte], A]
  type ByteStreamDecoder[F[_], A] =
    smithy4s.codecs.Reader[F, Stream[F, Byte], A]

}
