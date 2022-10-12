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

package smithy4s.schema

import smithy4s.ShapeId
import smithy4s.Refinement
import smithy4s.Bijection
import smithy4s.Lazy
import smithy4s.Hints
import Schema._

private[schema] object SchemaEqualityVisitor {

  def areEqual[A](left: Schema[_], right: Schema[_]): Boolean = {
    left.compile(new SchemaEqualityVisitor).apply(Set.empty, right)
  }

  type RecursiveContext = Set[ShapeId]
  type Check[A] = (RecursiveContext, Schema[_]) => Boolean

}

private[schema] class SchemaEqualityVisitor
    extends SchemaVisitor[SchemaEqualityVisitor.Check] { visit =>

  private def checking[A](
      f: PartialFunction[(Set[ShapeId], Schema[_]), Boolean]
  ): SchemaEqualityVisitor.Check[A] = { (context, schema) =>
    val tuple = (context, schema)
    if (f.isDefinedAt(tuple)) f(tuple)
    else false
  }

  // private val visitedLazy: java.util.HashSet[ShapeId] =
  //   new java.util.HashSet[ShapeId]()

  def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): SchemaEqualityVisitor.Check[P] = checking {
    case (_, PrimitiveSchema(`shapeId`, `hints`, otherTag)) =>
      tag == otherTag
  }

  def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): SchemaEqualityVisitor.Check[C[A]] = {
    val mEq = visit(member)

    checking {
      case (
            context,
            CollectionSchema(`shapeId`, `hints`, otherTag, otherMember)
          ) =>
        otherTag == tag && mEq(context, otherMember)
    }
  }

  def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): SchemaEqualityVisitor.Check[Map[K, V]] = {
    val kEq = visit(key)
    val vEq = visit(value)

    checking { case (c, MapSchema(`shapeId`, `hints`, otherKey, otherValue)) =>
      kEq(c, otherKey) && vEq(c, otherValue)
    }
  }

  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): SchemaEqualityVisitor.Check[E] = checking {
    case (_, EnumerationSchema(`shapeId`, `hints`, `values`, `total`)) => true
  }

  def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): SchemaEqualityVisitor.Check[S] = {
    def processField[AA](
        field: SchemaField[S, AA]
    ): (Set[ShapeId], SchemaField[_, _]) => Boolean = {
      val oEq =
        visit(field.instance).asInstanceOf[(Set[ShapeId], Schema[_]) => Boolean]
      (c, other) => {
        other.label == field.label &&
        oEq(c, other.instance) &&
        field.get == other.get
      }
    }
    val fieldsEq: (Set[ShapeId], Vector[SchemaField[_, _]]) => Boolean = {
      val allEq = fields.map(processField(_))
      (c, otherFields) => {
        otherFields.size == allEq.size && {
          allEq
            .zip(otherFields)
            .map { case (eq, otherField) => (eq(c, otherField)) }
            .forall(identity[Boolean])
        }
      }
    }
    checking {
      // Not comparing constructor because passing a type-safe constructor results
      // in a transformation into a generic product constructor (taking IndexedSeq[Any]
      // as an input), which is not stable wrt hashCode/equality
      case (c, Schema.StructSchema(`shapeId`, `hints`, fields, _)) =>
        fieldsEq(c, fields)
    }
  }

  def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): SchemaEqualityVisitor.Check[U] = {
    def processAlt[AA](
        field: SchemaAlt[U, AA]
    ): (Set[ShapeId], SchemaAlt[_, _]) => Boolean = {
      val oEq =
        visit(field.instance).asInstanceOf[(Set[ShapeId], Schema[_]) => Boolean]
      (c, other) => {
        other.label == field.label &&
        oEq(c, other.instance) &&
        field.inject == other.inject
      }
    }
    val altsEq: (Set[ShapeId], Vector[SchemaAlt[_, _]]) => Boolean = {
      val allEq = alternatives.map(processAlt(_))
      (c, otherFields) =>
        otherFields.size == allEq.size && {
          allEq
            .zip(otherFields)
            .map { case (eq, otherField) => (eq(c, otherField)) }
            .forall(identity[Boolean])
        }
    }
    checking {
      // Not comparing dispatchers because they are created dynamically in a
      // way that is unstable wrt hashCode/equality.
      case (c, Schema.UnionSchema(`shapeId`, `hints`, alternatives, _)) =>
        altsEq(c, alternatives)
    }
  }

  def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): SchemaEqualityVisitor.Check[B] = {
    val eq = visit(schema)
    checking { case (c, BijectionSchema(s, otherBijection)) =>
      eq(c, s) && bijection == otherBijection
    }
  }

  def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): SchemaEqualityVisitor.Check[B] = {
    val eq = visit(schema)
    checking { case (c, RefinementSchema(s, otherRefinement)) =>
      eq(c, s) && refinement == otherRefinement
    }
  }

  def lazily[A](suspend: Lazy[Schema[A]]): SchemaEqualityVisitor.Check[A] = {
    val underlyingSchema = suspend.value
    lazy val eq = visit(underlyingSchema)
    val shapeId = underlyingSchema.shapeId
    // We're only traversing the recursion fence "once". If we only traversed it,
    // we assume the comparison is correct
    checking { case (c, ls: LazySchema[_]) =>
      if (!c.contains(underlyingSchema.shapeId)) {
        val newC = c + shapeId
        eq(newC, ls.suspend.value)
      } else true
    }
  }

}
