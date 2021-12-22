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

object enumeration {

  /**
    * Provides schematic functions to model enumerations.
    */
  trait Schematic[F[_]] {
    def enumeration[A](
        to: A => (String, Int),
        fromName: Map[String, A],
        fromOrdinal: Map[Int, A]
    ): F[A]
  }

  class Schema[S[x[_]] <: Schematic[x], A](
      to: A => (String, Int),
      fromName: Map[String, A],
      fromOrdinal: Map[Int, A]
  ) extends schematic.Schema[S, A] {
    def compile[F[_]](s: S[F]): F[A] =
      s.enumeration(to, fromName, fromOrdinal)
  }

  trait Syntax {
    def enumeration[H, S[x[_]] <: Schematic[x], A](
        to: A => (String, Int),
        fromName: Map[String, A],
        fromOrdinal: Map[Int, A]
    ): schematic.Schema[S, A] =
      new Schema[S, A](to, fromName, fromOrdinal)
  }

}
