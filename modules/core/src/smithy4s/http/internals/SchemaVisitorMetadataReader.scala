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
package http
package internals

import smithy4s.http.internals.MetaDecode.{
  EmptyMetaDecode,
  PutField,
  StringListMapMetaDecode,
  StringCollectionMetaDecode,
  StringMapMetaDecode,
  StringValueMetaDecode,
  StructureMetaDecode
}
import smithy4s.schema._

import java.{util => ju}
import scala.collection.mutable.{Map => MMap}

private[http] object SchemaVisitorMetadataReader
    extends SchemaVisitorMetadataReader

private[http] class SchemaVisitorMetadataReader()
    extends SchemaVisitor[MetaDecode]
    with ScalaCompat { self =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): MetaDecode[P] = {
    tag match {
      case Primitive.PShort  => MetaDecode.from("Short")(_.toShortOption)
      case Primitive.PInt    => MetaDecode.from("Int")(_.toIntOption)
      case Primitive.PFloat  => MetaDecode.from("Float")(_.toFloatOption)
      case Primitive.PLong   => MetaDecode.from("Long")(_.toLongOption)
      case Primitive.PDouble => MetaDecode.from("Double")(_.toDoubleOption)
      case Primitive.PBigInt => MetaDecode.fromUnsafe("BigInt")(BigInt(_))
      case Primitive.PBigDecimal =>
        MetaDecode.fromUnsafe("BigDecimal")(BigDecimal(_))
      case Primitive.PBoolean => MetaDecode.from("Boolean")(_.toBooleanOption)
      case Primitive.PString  => MetaDecode.fromUnsafe("String")(identity)
      case Primitive.PUUID =>
        MetaDecode.fromUnsafe[ju.UUID]("UUID")(ju.UUID.fromString)
      case Primitive.PByte => EmptyMetaDecode
      case Primitive.PBlob =>
        MetaDecode.fromUnsafe("Bytes")(string =>
          ByteArray(ju.Base64.getDecoder().decode(string))
        )
      case Primitive.PDocument => EmptyMetaDecode
      case Primitive.PTimestamp =>
        (
          hints.get(HttpBinding).map(_.tpe),
          hints.get(smithy.api.TimestampFormat)
        ) match {
          case (_, Some(format)) =>
            MetaDecode.from(Timestamp.showFormat(format))(str =>
              Timestamp.parse(str, format)
            )
          case (Some(HttpBinding.Type.QueryType), None) |
              (Some(HttpBinding.Type.PathType), None) =>
            val formatString =
              Timestamp.showFormat(smithy.api.TimestampFormat.DATE_TIME)
            // See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html?highlight=httpquery#httpquery-trait
            MetaDecode.from(formatString)(str =>
              Timestamp.parse(str, smithy.api.TimestampFormat.DATE_TIME)
            )
          case (Some(HttpBinding.Type.HeaderType), None) =>
            val formatString =
              Timestamp.showFormat(smithy.api.TimestampFormat.HTTP_DATE)
            // See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html?highlight=httpquery#httpheader-trait
            MetaDecode.from(formatString)(str =>
              Timestamp.parse(str, smithy.api.TimestampFormat.HTTP_DATE)
            )
          case (None, None) =>
            EmptyMetaDecode
        }
      case Primitive.PUnit =>
        MetaDecode.StructureMetaDecode(
          _ => Right(MMap.empty[String, Any]),
          Some(_ => Right(()))
        )
    }
  }

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): MetaDecode[C[A]] = {
    self(member) match {
      case MetaDecode.StringValueMetaDecode(f) =>
        MetaDecode.StringCollectionMetaDecode[C[A]] { it =>
          tag.fromIterator(it.map(f))
        }
      case _ => EmptyMetaDecode
    }
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): MetaDecode[Map[K, V]] = {
    (self(key), self(value)) match {
      case (StringValueMetaDecode(readK), StringValueMetaDecode(readV)) =>
        StringMapMetaDecode[Map[K, V]](map =>
          map.map { case (k, v) => (readK(k), readV(v)) }.toMap
        )
      case (StringValueMetaDecode(readK), StringCollectionMetaDecode(readV)) =>
        StringListMapMetaDecode[Map[K, V]](map =>
          map.map { case (k, v) => (readK(k), readV(v)) }.toMap
        )
      case _ => EmptyMetaDecode
    }
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): MetaDecode[E] = {
    MetaDecode
      .from(
        s"Enum[${values.map(_.stringValue).mkString(",")}]"
      )(string => values.find(_.stringValue == string).map(_.value))
  }

  private case class FieldDecode(
      fieldName: String,
      binding: HttpBinding,
      update: (Metadata, PutField) => Unit
  )

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): MetaDecode[S] = {
    val reservedQueries =
      fields
        .map(f => HttpBinding.fromHints(f.label, f.hints, hints))
        .collect { case Some(HttpBinding.QueryBinding(query)) =>
          query
        }
        .toSet

    def decodeField[A](
        field: SchemaField[S, A]
    ): Option[FieldDecode] = {
      val schema = field.instance
      val label = field.label
      val fieldHints = field.hints
      HttpBinding.fromHints(label, fieldHints, hints).map { binding =>
        val decoder: MetaDecode[_] =
          self(schema.addHints(Hints(binding)))
        val update = decoder
          .updateMetadata(binding, label, field.isOptional, reservedQueries)
        FieldDecode(label, binding, update)
      }
    }
    val fieldUpdates: Vector[FieldDecode] =
      fields.flatMap(f => decodeField(f))

    val partial = { (metadata: Metadata) =>
      val buffer = MMap.empty[String, Any]
      val putField: PutField = new PutField {
        def putRequired(fieldName: String, value: Any): Unit =
          buffer += (fieldName -> value)

        def putSome(fieldName: String, value: Any): Unit =
          buffer += (fieldName -> value)

        def putNone(fieldName: String): Unit = ()
      }
      var currentFieldName: String = null
      var currentBinding: HttpBinding = null
      try {
        fieldUpdates.foreach { case FieldDecode(fieldName, binding, update) =>
          currentFieldName = fieldName
          currentBinding = binding
          update(metadata, putField)
        }
        Right(buffer)
      } catch {
        case e: MetadataError => Left(e)
        case MetaDecode.MetaDecodeError(const) =>
          Left(const(currentFieldName, currentBinding))
        case ConstraintError(_, message) =>
          Left(
            MetadataError.FailedConstraint(
              currentFieldName,
              currentBinding,
              message
            )
          )
      }
    }

    val total =
      if (fieldUpdates.size < fields.size) None
      else
        Some { (metadata: Metadata) =>
          val buffer = Vector.newBuilder[Any]
          val putField: PutField = new PutField {
            def putRequired(fieldName: String, value: Any): Unit =
              buffer += value

            def putSome(fieldName: String, value: Any): Unit =
              buffer += Some(value)

            def putNone(fieldName: String): Unit = buffer += None
          }

          var currentFieldName: String = null
          var currentBinding: HttpBinding = null
          try {
            fieldUpdates.foreach {
              case FieldDecode(fieldName, binding, update) =>
                currentFieldName = fieldName
                currentBinding = binding
                update(metadata, putField)
            }
            Right(make(buffer.result()))
          } catch {
            case e: MetadataError => Left(e)
            case MetaDecode.MetaDecodeError(const) =>
              Left(const(currentFieldName, currentBinding))
            case ConstraintError(_, message) =>
              Left(
                MetadataError.FailedConstraint(
                  currentFieldName,
                  currentBinding,
                  message
                )
              )
          }
        }

    StructureMetaDecode(partial, total)
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: U => Alt.SchemaAndValue[U, _]
  ): MetaDecode[U] = EmptyMetaDecode

  override def biject[A, B](
      schema: Schema[A],
      to: A => B,
      from: B => A
  ): MetaDecode[B] = self(schema).map(to)

  override def surject[A, B](
      schema: Schema[A],
      to: Refinement[A, B],
      from: B => A
  ): MetaDecode[B] = self(schema).map(to.asThrowingFunction)

  override def lazily[A](suspend: Lazy[Schema[A]]): MetaDecode[A] =
    EmptyMetaDecode
}
