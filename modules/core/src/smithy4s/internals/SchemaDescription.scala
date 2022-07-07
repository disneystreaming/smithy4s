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
package internals

import smithy4s.schema.SchemaVisitor
import smithy4s.schema.{Primitive, EnumValue, SchemaField, SchemaAlt, Alt}

object SchemaDescription extends SchemaVisitor[SchemaDescription] {
  type T[A] = String

  def of[A](value: String): SchemaDescription[A] = value
  // format: off
  override def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]): SchemaDescription[P] = {
    val value = tag match {
      case Primitive.PShort      => "Short"
      case Primitive.PInt        => "Int"
      case Primitive.PFloat      => "Float"
      case Primitive.PLong       => "Long"
      case Primitive.PDouble     => "Double"
      case Primitive.PBigInt     => "BigInt"
      case Primitive.PBigDecimal => "BigDecimal"
      case Primitive.PBoolean    => "Boolean"
      case Primitive.PString     => "String"
      case Primitive.PUUID       => "UUID"
      case Primitive.PByte       => "Byte"
      case Primitive.PBlob       => "Bytes"
      case Primitive.PDocument   => "Document"
      case Primitive.PTimestamp  => "Timestamp"
      case Primitive.PUnit       => "Unit"
    }
    SchemaDescription.of(value)
  }
  override def list[A](shapeId: ShapeId, hints: Hints, member: Schema[A]): SchemaDescription[List[A]] = {
    SchemaDescription.of("List")
  }
  override def set[A](shapeId: ShapeId, hints: Hints, member: Schema[A]): SchemaDescription[Set[A]] = {
    SchemaDescription.of("Set")
  }
  override def map[K, V](shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]): SchemaDescription[Map[K,V]] = {
    SchemaDescription.of("Map")
  }
  override def enumeration[E](shapeId: ShapeId, hints: Hints, values: List[EnumValue[E]], total: E => EnumValue[E]): SchemaDescription[E] =
    SchemaDescription.of("Enumeration")
  
  override def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[SchemaField[S, _]], make: IndexedSeq[Any] => S): SchemaDescription[S] =
    SchemaDescription.of("Structure")
  
  override def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[SchemaAlt[U, _]], dispatch: U => Alt.SchemaAndValue[U, _]): SchemaDescription[U] =
    SchemaDescription.of("Union")
  
  override def biject[A, B](schema: Schema[A], to: A => B, from: B => A): SchemaDescription[B] =
    SchemaDescription.of("Bijection")
  override def surject[A, B](schema: Schema[A], to: Refinement[A,B], from: B => A): SchemaDescription[B] =
    SchemaDescription.of("Surjection")
  override def lazily[A](suspend: Lazy[Schema[A]]): SchemaDescription[A] =
    SchemaDescription.of("Lazy")
  // format: on
}
