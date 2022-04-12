/*
 *  Copyright 2021 Disney Streaming
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
import smithy4s.internals.InputOutput
import smithy4s.internals.DiscriminatedUnionMember

final case class codecs(hintMask: HintMask = codecs.defaultHintMask)
    extends JsonCodecAPI(HintMask.mask(codecs.schematicJCodec, hintMask))

object codecs {

  val defaultHintMask: HintMask =
    HintMask(
      JsonName,
      TimestampFormat,
      Discriminated,
      InputOutput,
      DiscriminatedUnionMember
    )

  private[smithy4s] val schematicJCodec: Schematic[JCodec.JCodecMake] =
    new SchematicJCodec(Constraints.defaultConstraints, maxArity = 1024)

}
