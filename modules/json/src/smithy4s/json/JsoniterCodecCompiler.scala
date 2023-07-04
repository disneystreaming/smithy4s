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
  def withFlexibleCollectionsSupport(flexibleCollectionsSupport: Boolean): JsoniterCodecCompiler
  def withInfinitySupport(infinitySupport: Boolean): JsoniterCodecCompiler
  def withHintMask(hintMask: HintMask): JsoniterCodecCompiler

}
