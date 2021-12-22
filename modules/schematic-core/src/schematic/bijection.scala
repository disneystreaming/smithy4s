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
  * Provides capabilities to express a bijection between a type contained within
  * the metamodel and one that is not.
  */
object bijection {

  trait Schematic[F[_]] {
    def bijection[A, B](f: F[A], to: A => B, from: B => A): F[B]
  }

  class Schema[S[x[_]] <: Schematic[x], A, B](
      schema: schematic.Schema[S, A],
      to: A => B,
      from: B => A
  ) extends schematic.Schema[S, B] {
    def compile[F[_]](s: S[F]): F[B] = s.bijection(schema.compile(s), to, from)
  }

  trait OpenSyntax {
    def bijection[S[x[_]] <: Schematic[x], A, B](
        schema: schematic.Schema[S, A],
        to: A => B,
        from: B => A
    ): schematic.Schema[S, B] =
      new Schema(schema, to, from)
  }

  trait ClosedSyntax[S[x[_]] <: Schematic[x]] {
    def bijection[A, B](
        schema: schematic.Schema[S, A],
        to: A => B,
        from: B => A
    ): schematic.Schema[S, B] =
      new Schema(schema, to, from)
  }

}
