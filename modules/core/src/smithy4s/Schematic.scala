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

import schematic.Field

trait Schematic[F[_]]
    extends schematic.short.Schematic[F]
    with schematic.int.Schematic[F]
    with schematic.long.Schematic[F]
    with schematic.double.Schematic[F]
    with schematic.float.Schematic[F]
    with schematic.bigint.Schematic[F]
    with schematic.bigdecimal.Schematic[F]
    with schematic.string.Schematic[F]
    with schematic.boolean.Schematic[F]
    with schematic.uuid.Schematic[F]
    with schematic.byte.Schematic[F]
    with schematic.bytes.Schematic[F]
    with schematic.unit.Schematic[F]
    with schematic.list.Schematic[F]
    with schematic.set.Schematic[F]
    with schematic.vector.Schematic[F]
    with schematic.map.Schematic[F]
    with schematic.struct.Schematic[F]
    with schematic.union.Schematic[F]
    with schematic.enumeration.Schematic[F]
    with schematic.suspended.Schematic[F]
    with schematic.bijection.Schematic[F]
    with Timestamp.Schematic[F]
    with Hints.Schematic[F]
    with Document.Schematic[F]

trait StubSchematic[F[_]] extends Schematic[F] with schematic.StubSchematic[F] {
  def document: F[Document] = default
  def timestamp: F[Timestamp] = default
  def withHints[A](fa: F[A], hints: Hints): F[A] = default
  def struct[S](fields: Vector[Field[F, S, _]])(
      const: Vector[Any] => S
  ): F[S] = default
}
