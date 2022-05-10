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
package dynamic

import cats.Id
import java.util.UUID
import smithy4s.schema.Field
import smithy4s.schema.Alt

object DefaultSchematic extends smithy4s.Schematic[Id] {

  def short: Id[Short] = 0
  def int: Id[Int] = 0
  def long: Id[Long] = 0
  def double: Id[Double] = 0
  def float: Id[Float] = 0
  def bigint: Id[BigInt] = 0

  def bigdecimal: Id[BigDecimal] = 0

  def string: Id[String] = ""

  def boolean: Id[Boolean] = true

  def uuid: Id[UUID] = new UUID(0, 0)

  def byte: Id[Byte] = 0.toByte

  def bytes: Id[ByteArray] = ByteArray(Array.emptyByteArray)

  def unit: Id[Unit] = ()

  def list[S](fs: Id[S]): Id[List[S]] = List.empty

  def set[S](fs: Id[S]): Id[Set[S]] = Set.empty

  def vector[S](fs: Id[S]): Id[Vector[S]] = Vector.empty

  def map[K, V](fk: Id[K], fv: Id[V]): Id[Map[K, V]] = Map.empty

  def struct[S](fields: Vector[Field[Id, S, _]])(
      const: Vector[Any] => S
  ): Id[S] = const(fields.map(_.fold(new Field.Folder[Id, S, Any] {
    def onRequired[A](label: String, instance: Id[A], get: S => A): Any =
      instance

    def onOptional[A](
        label: String,
        instance: Id[A],
        get: S => Option[A]
    ): Any =
      None
  })))

  def union[S](first: Alt[Id, S, _], rest: Vector[Alt[Id, S, _]])(
      total: S => Alt.WithValue[Id, S, _]
  ): Id[S] = {
    def processAlt[A](alt: Alt[Id, S, A]) = alt.inject(alt.instance)
    processAlt(first)
  }

  def enumeration[A](
      to: A => (String, Int),
      fromName: Map[String, A],
      fromOrdinal: Map[Int, A]
  ): Id[A] = fromName.head._2

  def suspend[A](f: Lazy[Id[A]]): Id[A] = f.value

  def bijection[A, B](f: Id[A], to: A => B, from: B => A): Id[B] = to(f)

  def surjection[A, B](
      f: Id[A],
      to: smithy4s.Refinement[A, B],
      from: B => A
  ): Id[B] = to.unchecked(f)

  def timestamp: Id[Timestamp] = Timestamp.fromEpochSecond(0L)

  def withHints[A](fa: Id[A], hints: Hints): Id[A] = fa

  def document: Id[Document] = Document.DNull

}
