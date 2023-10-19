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

package smithy4s.http

import smithy4s.http.internals.HostPrefixSchemaVisitor
import smithy4s.codecs.Writer
import smithy4s.schema.OperationSchema

object HttpHostPrefix {
  def apply[I, E, O, SI, SO](
      endpoint: OperationSchema[I, E, O, SI, SO]
  ): Option[Writer[List[String], I]] = {
    for {
      endpointHint <- endpoint.hints.get(smithy.api.Endpoint)
      hostPrefixEncoder <- HostPrefixSchemaVisitor(
        endpoint.input.addHints(endpointHint)
      )
    } yield hostPrefixEncoder
  }

}
