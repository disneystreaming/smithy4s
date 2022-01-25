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

import java.{util => ju}

trait StubSchematic[F[_]]
    extends Schematic.all.Mixin[F]
    with javatime.StubSchematic[F] {

  def default[A]: F[A]

  def unit: F[Unit] = default

  def short: F[Short] = default

  def int: F[Int] = default

  def long: F[Long] = default

  def double: F[Double] = default

  def float: F[Float] = default

  def bigint: F[BigInt] = default

  def bigdecimal: F[BigDecimal] = default

  def string: F[String] = default

  def boolean: F[Boolean] = default

  def uuid: F[ju.UUID] = default

  def byte: F[Byte] = default

  def bytes: F[ByteArray] = default

  def list[S](fs: F[S]): F[List[S]] = default

  def set[S](fs: F[S]): F[Set[S]] = default

  def vector[S](fs: F[S]): F[Vector[S]] = default

  def map[K, V](fk: F[K], fv: F[V]): F[Map[K, V]] = default

  def union[S](first: Alt[F, S, _], rest: Vector[Alt[F, S, _]])(
      total: S => Alt.WithValue[F, S, _]
  ): F[S] = default

  def enumeration[A](
      to: A => (String, Int),
      fromName: Map[String, A],
      fromOrdinal: Map[Int, A]
  ): F[A] =
    default

  def suspend[A](f: => F[A]): F[A] = default

  def bijection[A, B](f: F[A], to: A => B, from: B => A): F[B] = default

  def struct[S](fields: Vector[Field[F, S, _]]): F[S] =
    default

}
