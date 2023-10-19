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

package smithy4s.decline.core

import smithy4s._

object CoreHints {

  type FieldName = FieldName.Type

  object FieldName extends Newtype[String] {
    val schema: Schema[Type] = Schema.bijection(Schema.string, apply, _.value)
    def id: ShapeId = ShapeId("smithy4s.cli", "FieldName")

    def require(
        hints: Hints
    ): FieldName =
      hints.get[FieldName].getOrElse(sys.error("Unknown field name!"))

  }

  type IsNested = IsNested.Type

  object IsNested extends Newtype[Boolean] {
    val schema: Schema[Type] = Schema.bijection(Schema.boolean, apply, _.value)
    def id: ShapeId = ShapeId("smithy4s.cli", "IsNested")

    def orFalse(hints: Hints): Boolean =
      hints.get(IsNested).fold(false)(_.value)
  }

}
