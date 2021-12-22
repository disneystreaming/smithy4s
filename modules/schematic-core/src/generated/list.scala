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

object list {

  class Schema[S[x[_]] <: Schematic[x], A](
      a: schematic.Schema[S, A])
      extends schematic.Schema[S, List[A]] {
    def compile[F[_]](s: S[F]): F[List[A]] =
      s.list(a.compile(s))
  }

  trait Schematic[F[_]] {
    def list[S](fs: F[S]): F[List[S]]
  }

  trait OpenSyntax {
    def list[S[x[_]] <: Schematic[x], A](
        a: schematic.Schema[S, A]): schematic.Schema[S, List[A]] =
      new Schema[S, A](a)
  }

  trait ClosedSyntax[S[x[_]] <: Schematic[x]] {
    def list[A](
        a: schematic.Schema[S, A]): schematic.Schema[S, List[A]] =
      new Schema[S, A](a)
  }
}
