package smithy4s

import smithy4s.kinds.PolyFunction

package object codecs {

  type PayloadReader[A] = Reader[Either[PayloadError, *], Blob, A]
  type PayloadWriter[A] = Writer[Unit, Blob, A]
  type PayloadCodec[A] = ReaderWriter[PayloadReader, PayloadWriter, A]

  object PayloadCodec {
    val readerK: PolyFunction[PayloadCodec, PayloadReader] =
      ReaderWriter.readerK[PayloadReader, PayloadWriter]

    val writerK: PolyFunction[PayloadCodec, PayloadWriter] =
      ReaderWriter.writerK[PayloadReader, PayloadWriter]
  }

}
