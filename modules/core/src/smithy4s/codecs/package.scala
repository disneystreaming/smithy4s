package smithy4s

import smithy4s.kinds.PolyFunction
import smithy4s.schema.CachedSchemaCompiler

package object codecs {

  type PayloadReader[A] = Reader[Either[PayloadError, *], Blob, A]
  object PayloadReader {
    type CachedCompiler = CachedSchemaCompiler[PayloadReader]
  }

  type PayloadWriter[A] = Writer[Unit, Blob, A]
  object PayloadWriter {
    type CachedCompiler = CachedSchemaCompiler[PayloadWriter]
  }

  type PayloadCodec[A] = ReaderWriter[PayloadReader, PayloadWriter, A]

  object PayloadCodec {
    type CachedCompiler = CachedSchemaCompiler[PayloadCodec]

    val readerK: PolyFunction[PayloadCodec, PayloadReader] =
      ReaderWriter.readerK[PayloadReader, PayloadWriter]

    val writerK: PolyFunction[PayloadCodec, PayloadWriter] =
      ReaderWriter.writerK[PayloadReader, PayloadWriter]
  }

}
