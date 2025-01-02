/*
 *  Copyright 2021-2025 Disney Streaming
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

object SimpleRestJsonBuilder
    extends SimpleRestJsonBuilder(
      new internals.SimpleRestJsonCodecs(
        jsonCodecs = Json.payloadCodecs,
        explicitDefaultsEncoding = false,
        hostPrefixInjection = true
      )
    )

class SimpleRestJsonBuilder private (
    simpleRestJsonCodecs: internals.SimpleRestJsonCodecs
) extends SimpleProtocolBuilder[alloy.SimpleRestJson](
      simpleRestJsonCodecs
    ) {

  @deprecated(message = "Use .withXXX methods instead", since = "0.18.25")
  def this(
      maxArity: Int,
      explicitDefaultsEncoding: Boolean,
      hostPrefixInjection: Boolean
  ) =
    this(
      new internals.SimpleRestJsonCodecs(
        Json.payloadCodecs
          .withJsoniterCodecCompiler(
            Json.jsoniter
              .withMaxArity(maxArity)
              .withExplicitDefaultsEncoding(explicitDefaultsEncoding)
          ),
        explicitDefaultsEncoding,
        hostPrefixInjection
      )
    )

  def withMaxArity(maxArity: Int): SimpleRestJsonBuilder =
    new SimpleRestJsonBuilder(
      simpleRestJsonCodecs.transformJsonCodecs(
        _.configureJsoniterCodecCompiler(_.withMaxArity(maxArity))
      )
    )

  def withExplicitDefaultsEncoding(
      explicitDefaultsEncoding: Boolean
  ): SimpleRestJsonBuilder =
    new SimpleRestJsonBuilder(
      simpleRestJsonCodecs.withExplicitDefaultEncoding(explicitDefaultsEncoding)
    )

  def disableHostPrefixInjection(): SimpleRestJsonBuilder =
    new SimpleRestJsonBuilder(
      simpleRestJsonCodecs.withHostPrefixInjection(false)
    )

  /**
    * Transforms the underlying JSON codec compiler to change its behaviour.
    */
  def transformJsonCodecs(
      f: JsonPayloadCodecCompiler => JsonPayloadCodecCompiler
  ): SimpleRestJsonBuilder =
    new SimpleRestJsonBuilder(simpleRestJsonCodecs.transformJsonCodecs(f))
}
