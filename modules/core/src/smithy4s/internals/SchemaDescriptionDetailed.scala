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

import smithy4s.schema.{
  Primitive,
  EnumValue,
  SchemaField,
  SchemaAlt,
  Alt,
  SchemaVisitor,
  CollectionTag
}

trait SchemaDescriptionDetailed[A]
    extends (Set[ShapeId] => (Set[ShapeId], String)) {
  def mapResult[B](f: String => String): SchemaDescriptionDetailed[B] = {
    seen =>
      val (s1, desc) = apply(seen)
      (s1 ++ seen, f(desc))
  }
  def flatMapResult[B](
      f: String => SchemaDescriptionDetailed[B]
  ): SchemaDescriptionDetailed[B] = { seen =>
    val (s1, desc) = apply(seen)
    f(desc)(s1 ++ seen)
  }
}

object SchemaDescriptionDetailed
    extends SchemaVisitor[SchemaDescriptionDetailed] { self =>

  def of[A](shapeId: ShapeId, value: String): SchemaDescriptionDetailed[A] =
    s => (s + shapeId, value)

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): SchemaDescriptionDetailed[P] = {
    SchemaDescriptionDetailed.of(
      shapeId,
      SchemaDescription.apply(tag.schema(shapeId))
    )
  }
  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): SchemaDescriptionDetailed[C[A]] = {
    apply(member).mapResult(s => s"${tag.name}[$s]")
  }
  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): SchemaDescriptionDetailed[Map[K, V]] = {
    apply(key).flatMapResult { kDesc =>
      apply(value).mapResult { vDesc =>
        s"Map[$kDesc, $vDesc]"
      }
    }
  }
  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): SchemaDescriptionDetailed[E] = {
    val vDesc = values.map(e => e.stringValue).mkString(", ")
    SchemaDescriptionDetailed.of(shapeId, s"Enumeration{ $vDesc }")
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): SchemaDescriptionDetailed[S] = { seen =>
    def forField[T](sf: SchemaField[S, T]): (String, (Set[ShapeId], String)) = {
      apply(sf.instance)(seen)
      sf.label -> apply(sf.instance)(seen)
    }
    val (sFinal, res) = fields
      .foldLeft((Set.empty[ShapeId], Seq.empty[(String, String)])) {
        case ((shapes, fieldDesc), field) =>
          val (label, (s2, desc)) = forField(field)
          (shapes ++ s2, fieldDesc :+ (label -> desc))
      }
    val fieldDesc =
      res.map { case (label, desc) => s"$label: $desc" }.mkString(", ")
    sFinal -> s"Structure ${shapeId.name}{ $fieldDesc }"
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: U => Alt.SchemaAndValue[U, _]
  ): SchemaDescriptionDetailed[U] = { seen =>
    def forAlt[T](alt: SchemaAlt[U, T]): (String, (Set[ShapeId], String)) = {
      val desc = apply(alt.instance)(seen)
      alt.label -> desc
    }
    val (sFinal, res) = alternatives
      .foldLeft((Set.empty[ShapeId], Seq.empty[(String, String)])) {
        case ((shapes, fieldDesc), alt) =>
          val (label, (s2, desc)) = forAlt(alt)
          (shapes ++ s2, fieldDesc :+ (label -> desc))
      }
    val fieldDesc =
      res.map { case (label, desc) => s"$label: $desc" }.mkString(", ")
    sFinal -> s"Union{ $fieldDesc }"
  }

  override def biject[A, B](
      schema: Schema[A],
      to: A => B,
      from: B => A
  ): SchemaDescriptionDetailed[B] = {
    apply(schema).mapResult { desc => s"Bijection{ $desc }" }
  }

  override def surject[A, B](
      schema: Schema[A],
      to: Refinement[A, B],
      from: B => A
  ): SchemaDescriptionDetailed[B] = {
    apply(schema).mapResult { desc => s"Surjection{ $desc }" }
  }
  override def lazily[A](
      suspend: Lazy[Schema[A]]
  ): SchemaDescriptionDetailed[A] = {
    new SchemaDescriptionDetailed[A] {
      val rec = suspend.map(s => s -> self.apply(s))
      override def apply(seen: Set[ShapeId]): (Set[ShapeId], String) = {
        val (schema, f) = rec.value
        if (seen(schema.shapeId)) {
          seen -> s"Recursive{ ${schema.shapeId.name} }"
        } else {
          f(seen + schema.shapeId)
        }
      }
    }
  }
}
