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

package smithy4s

// polyfill for the difference between Scala 2 and 3's unapply for case classes
// BINCOMPAT FOR 0.18 START
object UnapplyPolyfill {
  type Result[Tupled, CC] = Option[Tupled]
  def Result[Tupled, CC](f: Tupled => CC, a: Tupled): Option[Tupled] =
    Some(a)
}
// BINCOMPAT FOR 0.18 END
