package smithy4s.json

import com.github.plokhotnyuk.jsoniter_scala.core.JsonCodec
import smithy4s.HintMask

import smithy4s.schema.CachedSchemaCompiler

/**
  * A codec compiler that produces jsoniter's JsonCodec
  */
// scalafmt: {maxColumn = 120}
trait JsoniterCodecCompiler extends CachedSchemaCompiler[JsonCodec] {

  def withMaxArity(max: Int): JsoniterCodecCompiler
  def withExplicitNullEncoding(explicitNulls: Boolean): JsoniterCodecCompiler
  def withSparseCollectionsSupport(sparseCollectionSupport: Boolean): JsoniterCodecCompiler
  def withInfinitySupport(infinitySupport: Boolean): JsoniterCodecCompiler
  def withHintMask(hintMask: HintMask): JsoniterCodecCompiler

}
