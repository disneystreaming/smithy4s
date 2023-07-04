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
