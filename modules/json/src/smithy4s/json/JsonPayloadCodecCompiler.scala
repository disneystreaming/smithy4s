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

package smithy4s.json

import smithy4s.codecs.PayloadCodec
import smithy4s.schema.CachedSchemaCompiler

// scalafmt: {maxColumn = 120}
import com.github.plokhotnyuk.jsoniter_scala.core.{ReaderConfig => JsoniterReaderConfig}
import com.github.plokhotnyuk.jsoniter_scala.core.{WriterConfig => JsoniterWriterConfig}

trait JsonPayloadCodecCompiler extends CachedSchemaCompiler[PayloadCodec] {

  def withJsoniterCodecCompiler(jsoniterCodecCompiler: JsoniterCodecCompiler): JsonPayloadCodecCompiler
  def withJsoniterReaderConfig(jsoniterReaderConfig: JsoniterReaderConfig): JsonPayloadCodecCompiler
  def withJsoniterWriterConfig(jsoniterWriterConfig: JsoniterWriterConfig): JsonPayloadCodecCompiler

}
