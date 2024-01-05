/*
 *  Copyright 2021-2024 Disney Streaming
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

import smithy4s.schema.CachedSchemaCompiler

package object codecs {

  type BlobEncoder[A] = Encoder[Blob, A]
  object BlobEncoder {
    type Compiler = CachedSchemaCompiler[BlobEncoder]
    val noop: Compiler = new CachedSchemaCompiler.Uncached[BlobEncoder] {
      def fromSchema[A](schema: Schema[A]) = Encoder.static(Blob.empty)
    }
  }

  type BlobDecoder[A] = Decoder[Either[PayloadError, *], Blob, A]
  object BlobDecoder {
    type Compiler = CachedSchemaCompiler[BlobDecoder]
    val noop: Compiler = new CachedSchemaCompiler.Uncached[BlobDecoder] {
      def fromSchema[A](schema: Schema[A]) = Decoder.lift(_ =>
        Left(PayloadError(PayloadPath.root, "nothing", "always failing"))
      )
    }
  }

  type PayloadDecoder[A] = Decoder[Either[PayloadError, *], Blob, A]
  object PayloadDecoder {
    type CachedCompiler = CachedSchemaCompiler[PayloadDecoder]
  }

  type PayloadEncoder[A] = Encoder[Blob, A]
  object PayloadEncoder {
    type CachedCompiler = CachedSchemaCompiler[PayloadEncoder]
  }

}
