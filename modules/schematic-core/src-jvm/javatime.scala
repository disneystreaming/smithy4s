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

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

object javatime {

  trait Syntax
      extends instant.Syntax
      with localDate.Syntax
      with offsetDateTime.Syntax

  type Schematic[F[_]] = instant.Schematic[F]
    with localDate.Schematic[F]
    with offsetDateTime.Schematic[F]

  object Schematic {
    trait Mixin[F[_]]
        extends instant.Schematic[F]
        with localDate.Schematic[F]
        with offsetDateTime.Schematic[F]
  }

  trait StubSchematic[F[_]] extends Schematic.Mixin[F] {
    self: schematic.StubSchematic[F] =>
    def instant: F[Instant] = default

    def localDate: F[LocalDate] = default

    def offsetDateTime: F[OffsetDateTime] = default
  }

}
