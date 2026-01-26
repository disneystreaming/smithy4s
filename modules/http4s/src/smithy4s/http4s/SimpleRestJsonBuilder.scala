/*
 *  Copyright 2021-2026 Disney Streaming
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
package http4s

import smithy4s.json.Json
import smithy4s.json.JsonPayloadCodecCompiler
import smithy4s.schema.FieldFilter

object SimpleRestJsonBuilder
    extends SimpleRestJsonBuilder(
      new internals.SimpleRestJsonCodecs(
        jsonCodecs = Json.payloadCodecs,
        fieldFilter = FieldFilter.Default,
        hostPrefixInjection = true,
        smithyPathEncoding = false
      )
    )

class SimpleRestJsonBuilder private (
    simpleRestJsonCodecs: internals.SimpleRestJsonCodecs
) extends SimpleProtocolBuilder[alloy.SimpleRestJson](
      simpleRestJsonCodecs
    ) {

  def withMaxArity(maxArity: Int): SimpleRestJsonBuilder =
    new SimpleRestJsonBuilder(
      simpleRestJsonCodecs.transformJsonCodecs(
        _.configureJsoniterCodecCompiler(_.withMaxArity(maxArity))
      )
    )

  def withFieldFilter(
      fieldFilter: FieldFilter
  ): SimpleRestJsonBuilder =
    new SimpleRestJsonBuilder(
      simpleRestJsonCodecs.withFieldFilter(fieldFilter)
    )

  def withSmithyPathEncoding(enabled: Boolean): SimpleRestJsonBuilder =
    new SimpleRestJsonBuilder(
      simpleRestJsonCodecs.withSmithyPathEncoding(enabled)
    )

  def disableHostPrefixInjection(): SimpleRestJsonBuilder =
    new SimpleRestJsonBuilder(
      simpleRestJsonCodecs.withHostPrefixInjection(false)
    )

  /**
    * Transforms the underlying JSON codec compiler to change its behaviour.
    *
    * Note that the `.maxArity` transformation is only taken into consideration
    * for servers.
    */
  def transformJsonCodecs(
      f: JsonPayloadCodecCompiler => JsonPayloadCodecCompiler
  ): SimpleRestJsonBuilder =
    new SimpleRestJsonBuilder(simpleRestJsonCodecs.transformJsonCodecs(f))
}
