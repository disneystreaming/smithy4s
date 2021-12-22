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

object set {

  class Schema[S[x[_]] <: Schematic[x], A](
      a: schematic.Schema[S, A])
      extends schematic.Schema[S, Set[A]] {
    def compile[F[_]](s: S[F]): F[Set[A]] =
      s.set(a.compile(s))
  }

  trait Schematic[F[_]] {
    def set[S](fs: F[S]): F[Set[S]]
  }

  trait OpenSyntax {
    def set[S[x[_]] <: Schematic[x], A](
        a: schematic.Schema[S, A]): schematic.Schema[S, Set[A]] =
      new Schema[S, A](a)
  }

  trait ClosedSyntax[S[x[_]] <: Schematic[x]] {
    def set[A](
        a: schematic.Schema[S, A]): schematic.Schema[S, Set[A]] =
      new Schema[S, A](a)
  }
}
