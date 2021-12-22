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

package smithy4s

import schematic.OneOf
import schematic.union

object errorUnion {

  class Schema[E](
      val first: OneOf[Schematic, E, _],
      val rest: Vector[OneOf[Schematic, E, _]],
      val total: E => OneOf.WithValue[Schematic, E, _]
  ) extends schematic.Schema[Schematic, E] {

    val unionSchema = new union.Schema(first, rest, total)

    def find(label: String): Option[OneOf[Schematic, E, _]] = if (
      first.label == label
    ) Some(first)
    else
      rest.find(_.label == label)

    def compile[F[_]](s: Schematic[F]): F[E] =
      unionSchema.compile(s)

  }

  trait Syntax {
    def errors[E](
        first: OneOf[Schematic, E, _],
        rest: OneOf[Schematic, E, _]*
    )(
        total: E => OneOf.WithValue[Schematic, E, _]
    ): Schema[E] =
      new Schema(first, rest.toVector, total)
  }

}
