/*
 *  Copyright 2021-2024 Disney Streaming
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
package http.internals

import smithy4s.schema.Schema._

private[smithy4s] case class StaticUrlFormElements(
    elements: List[(String, String)]
)

private[smithy4s] object StaticUrlFormElements
    extends ShapeTag.Companion[StaticUrlFormElements] {

  val id: ShapeId = ShapeId("smithy4s.http.internals", "StaticUrlFormElements")

  val schema: Schema[StaticUrlFormElements] =
    list(tuple(string, string))
      .biject[StaticUrlFormElements](StaticUrlFormElements(_))(_.elements)

}
