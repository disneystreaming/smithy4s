/*
 *  Copyright 2021-2022 Disney Streaming
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
package schema

import java.util.UUID

class PassthroughSchematic[F[_]](schematic: Schematic[F]) extends Schematic[F] {
  def short: F[Short] = schematic.short

  def int: F[Int] = schematic.int

  def long: F[Long] = schematic.long

  def double: F[Double] = schematic.double

  def float: F[Float] = schematic.float

  def bigint: F[BigInt] = schematic.bigint

  def bigdecimal: F[BigDecimal] = schematic.bigdecimal

  def string: F[String] = schematic.string

  def boolean: F[Boolean] = schematic.boolean

  def uuid: F[UUID] = schematic.uuid

  def byte: F[Byte] = schematic.byte

  def bytes: F[ByteArray] = schematic.bytes

  def unit: F[Unit] = schematic.unit

  def collection[C[_], S](tag: CollectionTag[C, S], fs: F[S]): F[C[S]] =
    schematic.collection(tag, fs)

  def map[K, V](fk: F[K], fv: F[V]): F[Map[K, V]] = schematic.map(fk, fv)

  def struct[S](fields: Vector[Field[F, S, _]])(
      const: Vector[Any] => S
  ): F[S] = schematic.struct(fields)(const)

  def union[S](first: Alt[F, S, _], rest: Vector[Alt[F, S, _]])(
      total: S => Alt.WithValue[F, S, _]
  ): F[S] = schematic.union(first, rest)(total)

  def enumeration[A](
      to: A => (String, Int),
      fromName: Map[String, A],
      fromOrdinal: Map[Int, A]
  ): F[A] = schematic.enumeration(to, fromName, fromOrdinal)

  def suspend[A](f: Lazy[F[A]]): F[A] = schematic.suspend(f)

  def bijection[A, B](f: F[A], to: A => B, from: B => A): F[B] =
    schematic.bijection(f, to, from)

  def surjection[A, B](
      f: F[A],
      to: Refinement[A, B],
      from: B => A
  ): F[B] =
    schematic.surjection(f, to, from)

  def timestamp: F[Timestamp] = schematic.timestamp

  def withHints[A](fa: F[A], hints: Hints): F[A] =
    schematic.withHints(fa, hints)

  def document: F[Document] = schematic.document

}
