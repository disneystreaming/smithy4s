/*
 *  Copyright 2021 Disney Streaming
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

package schematic

/**
  * Represents a member of coproduct type (sealed trait)
  */
case class Alt[F[_], S, A](label: String, instance: F[A], inject: A => S) {
  def apply(value: A): Alt.WithValue[F, S, A] =
    Alt.WithValue(this, value)
}
object Alt {
  case class WithValue[F[_], S, A](alt: Alt[F, S, A], value: A)
}
