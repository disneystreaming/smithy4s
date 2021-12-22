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

object Schematic {

  type stdlib[F[_]] = all[F]

  object stdlib {
    trait Mixin[F[_]] extends all.Mixin[F]
  }

  type all[F[_]] = short.Schematic[F]
    with int.Schematic[F]
    with long.Schematic[F]
    with double.Schematic[F]
    with float.Schematic[F]
    with bigint.Schematic[F]
    with bigdecimal.Schematic[F]
    with string.Schematic[F]
    with boolean.Schematic[F]
    with uuid.Schematic[F]
    with byte.Schematic[F]
    with bytes.Schematic[F]
    with unit.Schematic[F]
    with list.Schematic[F]
    with set.Schematic[F]
    with vector.Schematic[F]
    with map.Schematic[F]
    with struct.Schematic[F]
    with union.Schematic[F]
    with enumeration.Schematic[F]
    with suspended.Schematic[F]
    with bijection.Schematic[F]
    with javatime.Schematic[F]

  object all {
    trait Mixin[F[_]]
        extends short.Schematic[F]
        with int.Schematic[F]
        with long.Schematic[F]
        with double.Schematic[F]
        with float.Schematic[F]
        with bigint.Schematic[F]
        with bigdecimal.Schematic[F]
        with string.Schematic[F]
        with boolean.Schematic[F]
        with uuid.Schematic[F]
        with byte.Schematic[F]
        with bytes.Schematic[F]
        with unit.Schematic[F]
        with list.Schematic[F]
        with set.Schematic[F]
        with vector.Schematic[F]
        with map.Schematic[F]
        with struct.Schematic[F]
        with union.Schematic[F]
        with enumeration.Schematic[F]
        with suspended.Schematic[F]
        with bijection.Schematic[F]
        with javatime.Schematic.Mixin[F]

  }
}
