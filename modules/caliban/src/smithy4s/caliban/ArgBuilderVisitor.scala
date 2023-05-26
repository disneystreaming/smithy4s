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

package smithy4s.caliban

import caliban.Value.NullValue
import cats.implicits._
import smithy4s.Bijection
import smithy4s.ByteArray
import smithy4s.Document
import smithy4s.Hints
import smithy4s.Refinement
import smithy4s.ShapeId
import smithy4s.Timestamp
import smithy4s.Schema
import smithy4s.schema.Field
import smithy4s.schema.Field.Wrapped
import smithy4s.schema.Primitive
import smithy4s.schema.SchemaVisitor
import smithy4s.schema.Alt
import smithy4s.schema.SchemaAlt
import caliban.schema.ArgBuilder
import caliban.InputValue
import caliban.CalibanError
import smithy4s.schema.{EnumTag, EnumValue}
import smithy4s.schema.EnumTag.StringEnum
import smithy4s.schema.EnumTag.IntEnum
import smithy4s.schema.CollectionTag
import caliban.Value

// todo: caching
private[caliban] object ArgBuilderVisitor
    extends SchemaVisitor.Default[ArgBuilder] {
  // todo: remaining cases
  override def default[A]: ArgBuilder[A] = sys.error("unsupported schema")

  override def biject[A, B](
      schema: smithy4s.Schema[A],
      bijection: Bijection[A, B]
  ): ArgBuilder[B] = schema.compile(this).map(bijection.to)

  override def refine[A, B](
      schema: smithy4s.schema.Schema[A],
      refinement: Refinement[A, B]
  ): ArgBuilder[B] = schema
    .compile(this)
    .flatMap(refinement(_).leftMap(e => CalibanError.ExecutionError(e)))

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[smithy4s.Schema, S, ?]],
      make: IndexedSeq[Any] => S
  ): ArgBuilder[S] = {
    val fieldsCompiled = fields.map { f =>
      f.label ->
        f.mapK(this)
          .instanceA(
            new Field.ToOptional[ArgBuilder] {
              override def apply[A0](
                  fa: ArgBuilder[A0]
              ): Wrapped[ArgBuilder, Option, A0] = ArgBuilder.option(fa)

            }
          )
    }

    { case InputValue.ObjectValue(objectFields) =>
      fieldsCompiled
        .traverse { case (label, f) =>
          f.build(objectFields.getOrElse(label, NullValue))
        }
        .map(make)
    // todo other cases
    }
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[smithy4s.schema.Schema, U]
  ): ArgBuilder[U] = {
    val instancesByKey =
      alternatives.map(alt => (alt.label -> handleAlt(alt))).toMap

    {
      case InputValue.ObjectValue(in) if in.sizeIs == 1 =>
        val (k, v) = in.head
        instancesByKey
          .get(k)
          .toRight(
            CalibanError.ExecutionError(msg = "Invalid union case: " + k)
          )
          .flatMap(_.build(v))
      // todo other cases
    }
  }
  private def handleAlt[U, A](alt: Alt[Schema, U, A]): ArgBuilder[U] =
    alt.instance.compile(this).map(alt.inject)

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): ArgBuilder[E] = tag match {
    case StringEnum =>
      val valuesByString = values.map(v => v.stringValue -> v).toMap

      ArgBuilder.string.flatMap { v =>
        valuesByString
          .get(v)
          .toRight(CalibanError.ExecutionError("Unknown enum case: " + v))
          .map(_.value)
      }

    case IntEnum =>
      val valuesByInt = values.map(v => v.intValue -> v).toMap
      ArgBuilder.int.flatMap { v =>
        valuesByInt
          .get(v)
          .toRight(CalibanError.ExecutionError("Unknown enum case: " + v))
          .map(_.value)
      }

  }

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): ArgBuilder[P] = {
    implicit val shortArgBuilder: ArgBuilder[Short] = _ => ??? // todo
    implicit val byteArgBuilder: ArgBuilder[Byte] = _ => ??? // todo
    implicit val byteArrayArgBuilder: ArgBuilder[ByteArray] = _ => ??? // todo
    implicit val documentArgBuilder: ArgBuilder[Document] = _ => ??? // todo
    implicit val timestampArgBuilder: ArgBuilder[Timestamp] = _ => ??? // todo

    Primitive.deriving[ArgBuilder].apply(tag)
  }

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): ArgBuilder[C[A]] = tag match {
    case CollectionTag.ListTag =>
      ArgBuilder.list(member.compile(this))

    case CollectionTag.IndexedSeqTag =>
      ArgBuilder.seq(member.compile(this)).map(_.toIndexedSeq)

    case CollectionTag.VectorTag =>
      ArgBuilder.seq(member.compile(this)).map(_.toVector)

    case CollectionTag.SetTag =>
      ArgBuilder.set(member.compile(this))
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): ArgBuilder[Map[K, V]] = {
    val keyBuilder = key.compile(this)
    val valueBuilder = value.compile(this)

    { case InputValue.ObjectValue(keys) =>
      keys.toList
        .traverse { case (k, v) =>
          (
            keyBuilder.build(Value.StringValue(k)),
            valueBuilder.build(v)
          ).tupled
        }
        .map(_.toMap)
    // todo other cases
    }
  }
}
