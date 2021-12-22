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
  * Provides schematic functions to model maps/dictionaries.
  */
object map {

  trait Schematic[F[_]] {
    def map[K, V](fk: F[K], fv: F[V]): F[Map[K, V]]
  }

  class Schema[S[x[_]] <: Schematic[x], K, V](
      key: schematic.Schema[S, K],
      value: schematic.Schema[S, V]
  ) extends schematic.Schema[S, Map[K, V]] {
    def compile[F[_]](s: S[F]): F[Map[K, V]] =
      s.map(key.compile(s), value.compile(s))
  }

  trait OpenSyntax {
    def map[S[x[_]] <: Schematic[x], K, V](
        key: schematic.Schema[S, K],
        value: schematic.Schema[S, V]
    ): schematic.Schema[S, Map[K, V]] =
      new Schema[S, K, V](key, value)

  }

  trait ClosedSyntax[S[x[_]] <: Schematic[x]] {
    def map[K, V](
        key: schematic.Schema[S, K],
        value: schematic.Schema[S, V]
    ): schematic.Schema[S, Map[K, V]] =
      new Schema[S, K, V](key, value)

  }

}
