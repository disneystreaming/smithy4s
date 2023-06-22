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
package http4s

import smithy4s.internals.InputOutput

object SimpleRestJsonBuilder extends SimpleRestJsonBuilder(1024, false)

class SimpleRestJsonBuilder private (
    maxArity: Int,
    explicitNullEncoding: Boolean
) extends SimpleProtocolBuilder[alloy.SimpleRestJson](
      new smithy4s.http.json.JsonCodecs(
        alloy.SimpleRestJson.protocol.hintMask ++ HintMask(
          InputOutput,
          IntEnum
        ),
        maxArity,
        explicitNullEncoding
      )
    ) {

  @deprecated("Use builder pattern instead of directly instantiating")
  def this(maxArity: Int) = this(maxArity, explicitNullEncoding = false)

  def withMaxArity(maxArity: Int): SimpleRestJsonBuilder =
    new SimpleRestJsonBuilder(maxArity, explicitNullEncoding)

  def withExplicitNullEncoding(
      explicitNullEncoding: Boolean
  ): SimpleRestJsonBuilder =
    new SimpleRestJsonBuilder(maxArity, explicitNullEncoding)
}
