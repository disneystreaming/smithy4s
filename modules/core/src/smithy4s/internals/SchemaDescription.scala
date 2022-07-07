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
  override def list[A](shapeId: ShapeId, hints: Hints, member: Schema[A]): SchemaDescription[List[A]] =
    SchemaDescription.of("List")
  
  override def set[A](shapeId: ShapeId, hints: Hints, member: Schema[A]): SchemaDescription[Set[A]] =
    SchemaDescription.of("Set")
  
  override def map[K, V](shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]): SchemaDescription[Map[K,V]] =
    SchemaDescription.of("Map")
  
  override def enumeration[E](shapeId: ShapeId, hints: Hints, values: List[EnumValue[E]], total: E => EnumValue[E]): SchemaDescription[E] =
    SchemaDescription.of("Enumeration")
  
  override def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[SchemaField[S, _]], make: IndexedSeq[Any] => S): SchemaDescription[S] =
    SchemaDescription.of("Structure")
  
  override def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[SchemaAlt[U, _]], dispatch: U => Alt.SchemaAndValue[U, _]): SchemaDescription[U] =
    SchemaDescription.of("Union")
  
  override def biject[A, B](schema: Schema[A], to: A => B, from: B => A): SchemaDescription[B] =
    SchemaDescription.of(apply(schema))
  override def surject[A, B](schema: Schema[A], to: Refinement[A,B], from: B => A): SchemaDescription[B] =
    SchemaDescription.of(apply(schema))
  override def lazily[A](suspend: Lazy[Schema[A]]): SchemaDescription[A] =
    suspend.map(s => SchemaDescription.of(apply(s))).value
}

trait SchemaDescriptionDetailed[A] extends (Set[ShapeId] => (Set[ShapeId], String)) {
  def mapResult[B](f: String => String): SchemaDescriptionDetailed[B] = { seen =>
    val (s1, desc) = apply(seen)
    (s1 ++ seen, f(desc))
  }
  def flatMapResult[B](f: String => SchemaDescriptionDetailed[B]): SchemaDescriptionDetailed[B] = { seen =>
    val (s1, desc) = apply(seen)
    f(desc)(s1 ++ seen)
  }
}

object SchemaDescriptionDetailed extends SchemaVisitor[SchemaDescriptionDetailed] {

  def of[A](shapeId: ShapeId, value: String): SchemaDescriptionDetailed[A] = s => (s + shapeId, value)

  override def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]): SchemaDescriptionDetailed[P] = {
    SchemaDescriptionDetailed.of(
      shapeId, 
      SchemaDescription.apply(tag.schema(shapeId))
    )
  }
  override def list[A](shapeId: ShapeId, hints: Hints, member: Schema[A]): SchemaDescriptionDetailed[List[A]] = {
    apply(member).mapResult(s => s"List[$s]")
  }
  override def set[A](shapeId: ShapeId, hints: Hints, member: Schema[A]): SchemaDescriptionDetailed[Set[A]] = {
    apply(member).mapResult(s => s"Set[$s]")
  }
  override def map[K, V](shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]): SchemaDescriptionDetailed[Map[K,V]] = {
    apply(key).flatMapResult { kDesc =>
      apply(value).mapResult { vDesc =>
        s"Map[$kDesc, $vDesc]"
      }
    }
  }
  override def enumeration[E](shapeId: ShapeId, hints: Hints, values: List[EnumValue[E]], total: E => EnumValue[E]): SchemaDescriptionDetailed[E] = {
    val vDesc = values.map(e => e.stringValue).mkString(", ")
    SchemaDescriptionDetailed.of(shapeId, s"Enumeration{ $vDesc }")
  }
    
  
  override def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[SchemaField[S, _]], make: IndexedSeq[Any] => S): SchemaDescriptionDetailed[S] = { seen =>
    def forField[T](sf: SchemaField[S, T]): (String, (Set[ShapeId], String)) = {
      apply(sf.instance)(seen)
      sf.label -> apply(sf.instance)(seen)
    }
    val (sFinal, res) = fields
      .map(f => forField(f))
      .foldLeft((Set.empty[ShapeId], Seq.empty[(String, String)])) { 
        case ((shapes, fieldDesc), (label, (s2, desc))) =>
          (shapes ++ s2, fieldDesc :+ (label -> desc))
      }
    val fieldDesc = res.map { case (label, desc) => s"$label: $desc" }.mkString(", ")
    sFinal -> s"Structure { $fieldDesc }"
  }
  
  override def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[SchemaAlt[U, _]], dispatch: U => Alt.SchemaAndValue[U, _]): SchemaDescriptionDetailed[U] = { seen =>
    def forAlt[T](alt: SchemaAlt[U, T]): (String, (Set[ShapeId], String)) = {
      val desc = apply(alt.instance)(seen)
      alt.label -> desc
    }
    val (sFinal, res) = alternatives
      .map(f => forAlt(f))
      .foldLeft((Set.empty[ShapeId], Seq.empty[(String, String)])) { 
        case ((shapes, fieldDesc), (label, (s2, desc))) =>
          (shapes ++ s2, fieldDesc :+ (label -> desc))
      }
    val fieldDesc = res.map { case (label, desc) => s"$label: $desc" }.mkString(", ")
    sFinal -> s"Union { $fieldDesc }"
  }
  
  override def biject[A, B](schema: Schema[A], to: A => B, from: B => A): SchemaDescriptionDetailed[B] = {
    apply(schema).mapResult { desc => s"Bijection { $desc }" }
  }
    
  override def surject[A, B](schema: Schema[A], to: Refinement[A,B], from: B => A): SchemaDescriptionDetailed[B] = {
    apply(schema).mapResult { desc => s"Surjection { $desc }" }
  }
  override def lazily[A](suspend: Lazy[Schema[A]]): SchemaDescriptionDetailed[A] = {
    // can't nail this one
    (seen: Set[ShapeId]) =>
      suspend.map { schema => 
        val (s2, desc) = apply(schema)(seen)
        if (seen(schema.shapeId)) {
          s2 -> s"Lazy { $desc }"
        } else {
          apply(schema)(seen ++ s2)
        }
      }
      .value
  }
// format: on
}
