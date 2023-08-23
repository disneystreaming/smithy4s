package smithy4s

import fs2.Stream

// scalafmt: {maxColumn = 120}
package object interopfs2 {

  type ByteStreamEncoder[F[_], A] =
    smithy4s.codecs.Writer[Any, Stream[F, Byte], A]
  type ByteStreamDecoder[F[_], A] =
    smithy4s.codecs.Reader[F, Stream[F, Byte], A]

}
