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

package smithy4s.interopcats

import cats.Show
import cats.implicits.toContravariantOps
import smithy4s._
import smithy4s.capability.EncoderK
import smithy4s.interopcats.instances.ShowInstances._
import smithy4s.schema.Schema
import smithy4s.schema._
import smithy4s.schema.Alt.Precompiler

object SchemaVisitorShow extends CachedSchemaCompiler.Impl[Show] {
  protected type Aux[A] = Show[A]
  def fromSchema[A](
      schema: Schema[A],
      cache: Cache
  ): Show[A] = {
    schema.compile(new SchemaVisitorShow(cache))
  }
}

final class SchemaVisitorShow(
    val cache: CompilationCache[Show]
) extends SchemaVisitor.Cached[Show] { self =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Show[P] = primShowPf(tag)

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Show[C[A]] = {
    implicit val showSchemaA: Show[A] = self(member)
    tag match {
      case CollectionTag.ListTag => Show[List[A]]

      case CollectionTag.SetTag => Show[Set[A]]

      case CollectionTag.VectorTag => Show[Vector[A]]

      case CollectionTag.IndexedSeqTag =>
        Show.show { seq =>
          seq.map(showSchemaA.show).mkString("IndexedSeq(", ", ", ")")
        }
    }
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): Show[U] = {

    val precomputed: Precompiler[Schema, Show] = new Precompiler[Schema, Show] {
      override def apply[A](label: String, instance: Schema[A]): Show[A] = {
        val showUnion = self(instance)
        (t: A) => s"${shapeId.name}($label = ${showUnion.show(t)})"
      }
    }
    implicit val encoderKShow: EncoderK[Show, String] =
      new EncoderK[Show, String] {
        override def apply[A](fa: Show[A], a: A): String = fa.show(a)

        override def absorb[A](f: A => String): Show[A] = Show.show(f)
      }
    dispatch.compile(precomputed)

  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): Show[B] = {
    self(schema).contramap(bijection.from)
  }

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): Show[B] =
    self(schema).contramap(refinement.from)

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): Show[Map[K, V]] = {
    implicit val showKey: Show[K] = self(key)
    implicit val showValue: Show[V] = self(value)
    Show[Map[K, V]]
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): Show[E] = Show.show { e =>
    total(e).stringValue
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): Show[S] = {
    def compileField[A](
        schemaField: SchemaField[S, A]
    ): S => String = {
      val folder = new Field.FolderK[Schema, S, Show]() {
        override def onRequired[AA](
            label: String,
            instance: Schema[AA],
            get: S => AA
        ): Show[AA] = self(instance)

        override def onOptional[AA](
            label: String,
            instance: Schema[AA],
            get: S => Option[AA]
        ): Show[Option[AA]] = {
          implicit val showAA: Show[AA] = self(instance)
          Show[Option[AA]]
        }
      }
      val showField = schemaField.foldK(folder)
      s => s"${schemaField.label} = ${showField.show(schemaField.get(s))}"
    }

    val functions = fields.map(f => compileField(f))
    Show.show { s =>
      val values = functions
        .map(f => f(s))
        .map { case (value) => s"$value" }
        .mkString("(", ", ", ")")
      s"${shapeId.name}$values"
    }
  }

  override def lazily[A](
      shapeId: ShapeId,
      hints: Hints,
      suspend: Lazy[Schema[A]]
  ): Show[A] = Show.show[A] {
    val ss = suspend.map {
      self(_)
    }
    a => ss.value.show(a)
  }

  override def nullable[A](
      shapeId: ShapeId,
      hints: Hints,
      schema: Schema[A]
  ): Show[Option[A]] = {
    val showA = self(schema)
    locally {
      case None        => "None"
      case Some(value) => s"Some(${showA.show(value)})"
    }
  }

}
