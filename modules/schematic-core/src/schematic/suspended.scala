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
  * Provides capabilities to suspend encoders/decoders, which is useful
  * for recursive datatypes
  */
object suspended {

  trait Schematic[F[_]] {
    def suspend[A](f: => F[A]): F[A]
  }

  class Schema[S[x[_]] <: Schematic[x], A](schema: => schematic.Schema[S, A])
      extends schematic.Schema[S, A] {

    private lazy val deferredSchema = schema

    def compile[F[_]](s: S[F]): F[A] = {
      s.suspend(deferredSchema.compile(s))
    }
  }

  trait OpenSyntax {
    def suspend[S[x[_]] <: Schematic[x], A](
        schema: => schematic.Schema[S, A]
    ): schematic.Schema[S, A] = {
      new Schema(schema)
    }

    def recursive[S[x[_]] <: Schematic[x], A](
        schema: => schematic.Schema[S, A]
    ): schematic.Schema[S, A] =
      new Schema(schema)
  }

  trait ClosedSyntax[S[x[_]] <: Schematic[x]] {
    def suspend[A](schema: => schematic.Schema[S, A]): schematic.Schema[S, A] =
      new Schema[S, A](schema)

    def recursive[A](
        schema: => schematic.Schema[S, A]
    ): schematic.Schema[S, A] =
      new Schema(schema)
  }

}
