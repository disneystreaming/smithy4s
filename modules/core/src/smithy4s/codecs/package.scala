package smithy4s

package object codecs {

  type PayloadReader[A] = Reader[Either[PayloadError, *], Blob, A]
  type PayloadWriter[A] = Writer[Unit, Blob, A]
  type PayloadCodec[A] = ReaderWriter[PayloadReader, PayloadWriter, A]

}
