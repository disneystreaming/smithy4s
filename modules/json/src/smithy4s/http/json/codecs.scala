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
package http.json

import smithy.api.JsonName
import smithy.api.TimestampFormat
import smithy4s.api.Discriminated
import smithy4s.api.Untagged
import smithy4s.internals.DiscriminatedUnionMember
import smithy4s.internals.InputOutput
import smithy4s.schema.SchemaVisitor
import smithy4s.schema.CompilationCache

final case class codecs(
    hintMask: HintMask = codecs.defaultHintMask,
    cache: CompilationCache[JCodec]
) extends JsonCodecAPI(codecs.schemaVisitorJCodec(cache), Some(hintMask))

object codecs {

  val defaultHintMask: HintMask =
    HintMask(
      JsonName,
      TimestampFormat,
      Discriminated,
      Untagged,
      InputOutput,
      DiscriminatedUnionMember,
      // TODO: add tests for `codecs` understanding int enums. Maybe pizza spec
      IntEnum
    )

  private[smithy4s] def schemaVisitorJCodec(
      cache: CompilationCache[JCodec]
  ): SchemaVisitor[JCodec] =
    new SchemaVisitorJCodec(maxArity = 1024, cache)

}
