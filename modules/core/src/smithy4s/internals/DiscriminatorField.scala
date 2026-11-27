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
package internals

/**
  * Extracts the name of the discriminator field of a union, if the union is
  * annotated with `alloy#discriminated` or `smithy.api#discriminated`.
  *
  * When both traits are present, `alloy#discriminated` takes precedence.
  */
private[smithy4s] object DiscriminatorField {

  def unapply(hints: Hints): Option[String] =
    hints
      .get(alloy.Discriminated)
      .map(_.value)
      .orElse(hints.get(smithy.api.Discriminated).map(_.value))

}
