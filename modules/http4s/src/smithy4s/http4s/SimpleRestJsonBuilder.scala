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
import smithy4s.schema.CompilationCache
import smithy4s.http.json.JCodec

object SimpleRestJsonBuilder
    extends SimpleProtocolBuilder[smithy4s.api.SimpleRestJson](
      smithy4s.http.json.codecs(
        smithy4s.api.SimpleRestJson.protocol.hintMask ++ HintMask(InputOutput),
        CompilationCache.make[JCodec]
      )
    )
