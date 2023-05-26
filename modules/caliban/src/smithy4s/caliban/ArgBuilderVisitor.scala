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
import smithy4s.Lazy
import java.util.Base64
import caliban.Value.StringValue
import caliban.Value.IntValue.IntNumber
import caliban.Value.FloatValue.BigDecimalNumber
import caliban.Value.IntValue.BigIntNumber
import caliban.Value.BooleanValue
import caliban.Value.FloatValue.FloatNumber
import caliban.Value.FloatValue.DoubleNumber
import caliban.Value.IntValue.LongNumber
import caliban.InputValue.ObjectValue
import caliban.InputValue.ListValue
import caliban.InputValue.VariableValue
import smithy.api.TimestampFormat

// todo: caching
private[caliban] object ArgBuilderVisitor extends SchemaVisitor[ArgBuilder] {

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
      val valuesByString = values.map(v => v.stringValue -> v.value).toMap

      ArgBuilder.string
        .flatMap { v =>
          valuesByString
            .get(v)
            .toRight(CalibanError.ExecutionError("Unknown enum case: " + v))
        }

    case IntEnum =>
      val valuesByInt = values.map(v => v.intValue -> v.value).toMap

      ArgBuilder.int
        .flatMap { v =>
          valuesByInt
            .get(v)
            .toRight(CalibanError.ExecutionError("Unknown int enum case: " + v))
        }

  }

  private val inputValueToDoc: PartialFunction[InputValue, Document] = {
    case StringValue(value)      => Document.fromString(value)
    case IntNumber(value)        => Document.fromInt(value)
    case BigDecimalNumber(value) => Document.fromBigDecimal(value)
    case BigIntNumber(value)     => Document.fromBigDecimal(BigDecimal(value))
    case BooleanValue(value)     => Document.fromBoolean(value)
    case FloatNumber(value)      => Document.fromDouble(value.toDouble)
    case DoubleNumber(value)     => Document.fromDouble(value)
    case LongNumber(value)       => Document.fromLong(value)
    case ObjectValue(fields) => Document.DObject(fields.fmap(inputValueToDoc))
    case ListValue(values)   => Document.array(values.map(inputValueToDoc))
  }
  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): ArgBuilder[P] = {
    // todo: these would probably benefit from a direct implementation
    implicit val shortArgBuilder: ArgBuilder[Short] =
      ArgBuilder.int.flatMap {
        case i if i.isValidShort => Right(i.toShort)
        case i =>
          Left(CalibanError.ExecutionError("Integer too large for short: " + i))
      }

    implicit val byteArgBuilder: ArgBuilder[Byte] = ArgBuilder.int.flatMap {
      case i if i.isValidByte => Right(i.toByte)
      case i =>
        Left(CalibanError.ExecutionError("Integer too large for byte: " + i))
    }

    implicit val byteArrayArgBuilder: ArgBuilder[ByteArray] =
      ArgBuilder.string.map { str =>
        ByteArray(Base64.getDecoder().decode(str))
      }

    implicit val documentArgBuilder: ArgBuilder[Document] = v =>
      inputValueToDoc
        .lift(v)
        .toRight(
          CalibanError.ExecutionError(
            s"Unsupported input value for Document: $v"
          )
        )

    implicit val timestampArgBuilder: ArgBuilder[Timestamp] = {
      hints.get(TimestampFormat) match {
        case Some(TimestampFormat.EPOCH_SECONDS) | None =>
          ArgBuilder.long.map(Timestamp.fromEpochSecond(_))

        case Some(format) =>
          ArgBuilder.string.flatMap(s =>
            Timestamp
              .parse(s, format)
              .toRight(
                CalibanError.ExecutionError(
                  s"Invalid timestamp for format $format: $s"
                )
              )
          )
      }
    }

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

  override def lazily[A](suspend: Lazy[Schema[A]]): ArgBuilder[A] = {
    val underlying = suspend.map(_.compile(this))

    underlying.value.build(_)
  }
}
