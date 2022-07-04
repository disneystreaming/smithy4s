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

import java.{util => ju}

trait StubSchematic[F[_]] extends Schematic[F] {

  def default[A]: F[A]

  override def unit: F[Unit] = default

  override def short: F[Short] = default

  override def int: F[Int] = default

  override def long: F[Long] = default

  override def double: F[Double] = default

  override def float: F[Float] = default

  override def bigint: F[BigInt] = default

  override def bigdecimal: F[BigDecimal] = default

  override def string: F[String] = default

  override def boolean: F[Boolean] = default

  override def uuid: F[ju.UUID] = default

  override def byte: F[Byte] = default

  override def bytes: F[ByteArray] = default

  override def document: F[Document] = default

  override def timestamp: F[Timestamp] = default

  override def withHints[A](fa: F[A], hints: Hints): F[A] = default

  override def collection[C[_], S](
      tag: CollectionTag[C, S],
      fs: F[S]
  ): F[C[S]] = default

  override def map[K, V](fk: F[K], fv: F[V]): F[Map[K, V]] = default

  override def union[S](first: Alt[F, S, _], rest: Vector[Alt[F, S, _]])(
      total: S => Alt.WithValue[F, S, _]
  ): F[S] = default

  override def enumeration[A](
      to: A => (String, Int),
      fromName: Map[String, A],
      fromOrdinal: Map[Int, A]
  ): F[A] =
    default

  override def suspend[A](f: Lazy[F[A]]): F[A] = default

  override def bijection[A, B](f: F[A], to: A => B, from: B => A): F[B] =
    default

  override def surjection[A, B](
      f: F[A],
      refinement: Refinement[A, B],
      from: B => A
  ): F[B] =
    default

  override def struct[S](fields: Vector[Field[F, S, _]])(
      const: Vector[Any] => S
  ): F[S] = default

}
