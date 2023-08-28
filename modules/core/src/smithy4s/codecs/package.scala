/*
 *  Copyright 2021-2022 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s

import smithy4s.kinds.PolyFunction
import smithy4s.schema.CachedSchemaCompiler

package object codecs {

  type Encoder[Out, A] = Writer[Any, Out, A]

  type BlobEncoder[A] = Encoder[Blob, A]
  object BlobEncoder {
    type Compiler = CachedSchemaCompiler[BlobEncoder]
    val noop: Compiler = new CachedSchemaCompiler.Uncached[BlobEncoder] {
      def fromSchema[A](schema: Schema[A]) = Writer.encodeBy(_ => Blob.empty)
    }
  }

  type BlobDecoder[A] = Reader[Either[PayloadError, *], Blob, A]
  object BlobDecoder {
    type Compiler = CachedSchemaCompiler[BlobDecoder]
    val noop: Compiler = new CachedSchemaCompiler.Uncached[BlobDecoder] {
      def fromSchema[A](schema: Schema[A]) = Reader.lift(_ =>
        Left(PayloadError(PayloadPath.root, "nothing", "always failing"))
      )
    }
  }

  type PayloadReader[A] = Reader[Either[PayloadError, *], Blob, A]
  object PayloadReader {
    type CachedCompiler = CachedSchemaCompiler[PayloadReader]
  }

  type PayloadWriter[A] = Writer[Any, Blob, A]
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
