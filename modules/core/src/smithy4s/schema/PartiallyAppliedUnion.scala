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

package smithy4s.schema

import smithy4s.Hints

final class PartiallyAppliedUnion[U](val alts: Vector[Alt[U, _]])
    extends AnyVal {

  def apply(f: U => Int): Schema.UnionSchema[U] =
    Schema.UnionSchema(Schema.placeholder, Hints.empty, alts, f)

  /**
   * A convenience method to build union schemas easily. It shouldn't be
   * used in real usecases.
   */
  def reflective: Schema.UnionSchema[U] = {
    def ordinal(u: U) = {
      var i = -1
      var found = false
      while ((i < alts.size) && !found) {
        i += 1
        found = alts(i).project.isDefinedAt(u)
      }
      if (found) i
      else throw new scala.MatchError(u)
    }
    Schema.UnionSchema(
      Schema.placeholder,
      Hints.empty,
      alts,
      ordinal
    )
  }

}
