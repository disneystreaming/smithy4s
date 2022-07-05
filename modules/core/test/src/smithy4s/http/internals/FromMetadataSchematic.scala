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
package http.internals

import smithy4s.ByteArray
import smithy4s.Document
import smithy4s.Hints
import smithy4s.Lazy
import smithy4s.Schematic
import smithy4s.Timestamp
import smithy4s.schema.CollectionTag
import smithy4s.schema.Alt
import smithy4s.schema.Field

import java.util.UUID

trait FromMetadata[+A] {
  def read(metadata: Map[String, Any]): Either[String, A]
}

object FromMetadata {

  def default: FromMetadata[Nothing] = (_: Map[String, Any]) =>
    Left("Only structs are supported")
}

object FromMetadataSchematic extends Schematic[FromMetadata] {
  def short: FromMetadata[Short] = FromMetadata.default

  def int: FromMetadata[Int] = FromMetadata.default

  def long: FromMetadata[Long] = FromMetadata.default

  def double: FromMetadata[Double] = FromMetadata.default

  def float: FromMetadata[Float] = FromMetadata.default

  def bigint: FromMetadata[BigInt] = FromMetadata.default

  def bigdecimal: FromMetadata[BigDecimal] = FromMetadata.default

  def string: FromMetadata[String] = FromMetadata.default

  def boolean: FromMetadata[Boolean] = FromMetadata.default

  def uuid: FromMetadata[UUID] = FromMetadata.default

  def byte: FromMetadata[Byte] = FromMetadata.default

  def bytes: FromMetadata[ByteArray] = FromMetadata.default

  def timestamp: FromMetadata[Timestamp] = FromMetadata.default

  def unit: FromMetadata[Unit] = FromMetadata.default

  def document: FromMetadata[Document] = FromMetadata.default

  def collection[C[_], S](
      tag: CollectionTag[C],
      fs: FromMetadata[S]
  ): FromMetadata[C[S]] = FromMetadata.default

  def map[K, V](
      fk: FromMetadata[K],
      fv: FromMetadata[V]
  ): FromMetadata[Map[K, V]] = FromMetadata.default

  def union[S](
      first: Alt[FromMetadata, S, _],
      rest: Vector[Alt[FromMetadata, S, _]]
  )(total: S => Alt.WithValue[FromMetadata, S, _]): FromMetadata[S] =
    FromMetadata.default

  def enumeration[A](
      to: A => (String, Int),
      fromName: Map[String, A],
      fromOrdinal: Map[Int, A]
  ): FromMetadata[A] = FromMetadata.default

  def suspend[A](f: Lazy[FromMetadata[A]]): FromMetadata[A] =
    FromMetadata.default

  def bijection[A, B](
      f: FromMetadata[A],
      to: A => B,
      from: B => A
  ): FromMetadata[B] = FromMetadata.default

  def surjection[A, B](
      f: FromMetadata[A],
      to: Refinement[A, B],
      from: B => A
  ): FromMetadata[B] = FromMetadata.default

  def withHints[A](fa: FromMetadata[A], hints: Hints): FromMetadata[A] = fa

  def struct[S](fields: Vector[Field[FromMetadata, S, _]])(
      const: Vector[Any] => S
  ): FromMetadata[S] = { (metadata: Map[String, Any]) =>
    fields
      .traverse { (field: Field[FromMetadata, S, _]) =>
        (metadata.get(field.label), field.isRequired) match {
          case (Some(value), true) =>
            Right(value)
          case (None, true) =>
            Left(s"Not found : ${field.label}")
          case (other, _) =>
            Right((other: Any))
        }
      }
      .map(const)
  }

}
