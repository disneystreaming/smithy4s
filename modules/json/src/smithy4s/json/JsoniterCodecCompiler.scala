/*
 *  Copyright 2021-2023 Disney Streaming
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
import smithy4s.internals._
import smithy4s.HintMask
import smithy.api._
import alloy._

import smithy4s.schema.CachedSchemaCompiler

/**
  * A codec compiler that produces jsoniter's JsonCodec
  */
// scalafmt: {maxColumn = 120}
trait JsoniterCodecCompiler extends CachedSchemaCompiler[JsonCodec] {

  /**
    * Changes the behaviour of the decoders so that they fail after
    * a certain number of elements when decoding arrays and maps. This
    * allows to protect against some DDOS attacks.
    *
    * Defaults to 1024.
    */
  def withMaxArity(max: Int): JsoniterCodecCompiler

  /**
    * Changes the behaviour of Json encoders so that optional values are encoded as
    * explicit Json null values.
    *
    * Defaults to false.
    */
  def withExplicitDefaultsEncoding(explicitNulls: Boolean): JsoniterCodecCompiler

  /**
   * Changes the behaviour of Json decoders so that they overlook null values in collections
   * and maps. This behaviour has a performance overhead.
   *
   * Defaults to false
   */
  def withFlexibleCollectionsSupport(flexibleCollectionsSupport: Boolean): JsoniterCodecCompiler

  /**
    * Changes the behaviour of Json decoders so that they can parse Infinity/NaN values.
    * This behaviour has a performance overhead.
    */
  def withInfinitySupport(infinitySupport: Boolean): JsoniterCodecCompiler

  /**
    * Changes the behaviour of Json decoders so that the preserve the ordering of maps.
    */
  def withMapOrderPreservation(preserveMapOrder: Boolean): JsoniterCodecCompiler

  /**
    * Changes the hint mask with which the decoder works. Depending on the hint mask, some
    * smithy traits may be overlooked during encoding/decoding. For instance, `@jsonName`.
    */
  def withHintMask(hintMask: HintMask): JsoniterCodecCompiler

}

object JsoniterCodecCompiler {

  val defaultMaxArity: Int = 1024

  val defaultHintMask: HintMask =
    HintMask(
      JsonName,
      TimestampFormat,
      Discriminated,
      Untagged,
      InputOutput,
      DiscriminatedUnionMember,
      Default,
      Required
    )

}
