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

private[internals] trait SchemaDescriptionDetailedImpl[A]
    extends (Set[ShapeId] => (Set[ShapeId], String)) {
  def mapResult[B](f: String => String): SchemaDescriptionDetailedImpl[B] = {
    seen =>
      val (s1, desc) = apply(seen)
      (s1 ++ seen, f(desc))
  }
  def flatMapResult[B](
      f: String => SchemaDescriptionDetailedImpl[B]
  ): SchemaDescriptionDetailedImpl[B] = { seen =>
    val (s1, desc) = apply(seen)
    f(desc)(s1 ++ seen)
  }
}

private[internals] object SchemaDescriptionDetailedImpl
    extends SchemaVisitor[SchemaDescriptionDetailedImpl] { self =>

  def of[A](shapeId: ShapeId, value: String): SchemaDescriptionDetailedImpl[A] =
    s => (s + shapeId, value)

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): SchemaDescriptionDetailedImpl[P] = {
    SchemaDescriptionDetailedImpl.of(
      shapeId,
      SchemaDescription.apply(tag.schema(shapeId))
    )
  }
  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): SchemaDescriptionDetailedImpl[C[A]] = {
    apply(member).mapResult(s => s"${tag.name}[$s]")
  }
  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): SchemaDescriptionDetailedImpl[Map[K, V]] = {
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
  ): SchemaDescriptionDetailedImpl[E] = {
    val vDesc = values.map(e => s""""${e.stringValue}"""").mkString(", ")
    SchemaDescriptionDetailedImpl.of(shapeId, s"enum($vDesc)")
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): SchemaDescriptionDetailedImpl[S] = { seen =>
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
    sFinal -> s"structure ${shapeId.name}($fieldDesc)"
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): SchemaDescriptionDetailedImpl[U] = { seen =>
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
      res.map { case (label, desc) => s"$label: $desc" }.mkString(" | ")
    sFinal -> s"union ${shapeId.name}($fieldDesc)"
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): SchemaDescriptionDetailedImpl[B] = {
    apply(schema).mapResult(identity)
  }

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): SchemaDescriptionDetailedImpl[B] = {
    apply(schema).mapResult { desc => s"Refinement[$desc]" }
  }
  override def lazily[A](
      suspend: Lazy[Schema[A]]
  ): SchemaDescriptionDetailedImpl[A] = {
    new SchemaDescriptionDetailedImpl[A] {
      val rec = suspend.map(s => s -> self.apply(s))
      override def apply(seen: Set[ShapeId]): (Set[ShapeId], String) = {
        val (schema, f) = rec.value
        if (seen(schema.shapeId)) {
          seen -> s"Recursive[${schema.shapeId.name}]"
        } else {
          f(seen + schema.shapeId)
        }
      }
    }
  }

  val conversion: SchemaDescriptionDetailedImpl ~> SchemaDescription =
    new PolyFunction[SchemaDescriptionDetailedImpl, SchemaDescription] {
      def apply[A](
          fa: SchemaDescriptionDetailedImpl[A]
      ): SchemaDescription[A] = fa(Set.empty)._2
    }
}
