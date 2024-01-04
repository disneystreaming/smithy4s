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

package smithy4s.kinds

object PolyFunctions {

  def mapErrorK[E, E1](f: E => E1): PolyFunction[Either[E, *], Either[E1, *]] =
    new PolyFunction[Either[E, *], Either[E1, *]] {
      def apply[A](either: Either[E, A]): Either[E1, A] = either.left.map(f)
    }

}
