package smithy4s

import _root_.fs2.Stream

package object fs2lib {

  type XmlByteStreamEncoder[F[_], A] =
    smithy4s.codecs.Writer[Any, Stream[F, Byte], A]
  type XmlByteStreamDecoder[F[_], A] =
    smithy4s.codecs.Reader[F, Stream[F, Byte], A]

}
