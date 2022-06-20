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

import smithy4s.http.HttpBinding
import smithy4s.http.internals.MetaEncode._
import smithy4s.schema.Alt.SchemaAndValue
import smithy4s.schema.{
  EnumValue,
  Field,
  Primitive,
  SchemaAlt,
  SchemaField,
  SchemaVisitor
}

import java.util.Base64

/**
 * This schema visitor works on data that is annotated with :
 * - smithy.api.httpLabel
 * - smithy.api.httpHeader
 * - smithy.api.httpPrefixHeaders
 * - smithy.api.httpQuery
 * - smithy.api.httpQueryParams
 *
 * As such, assumptions are made using the information of what types can be
 * annotated in the smithy specs.
 *
 */
object SchemaVisitorMetadataWriter extends SchemaVisitor[MetaEncode] { self =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): MetaEncode[P] = {
    tag match {
      case Primitive.PBlob =>
        StringValueMetaEncode[ByteArray](ba =>
          Base64.getEncoder.encodeToString(ba.array)
        )
      case Primitive.PTimestamp =>
        (
          hints.get(HttpBinding).map(_.tpe),
          hints.get(smithy.api.TimestampFormat)
        ) match {
          case (_, Some(timestampFormat)) =>
            StringValueMetaEncode((timestamp: Timestamp) =>
              timestamp.format(timestampFormat)
            )
          case (Some(HttpBinding.Type.QueryType), None) |
              (Some(HttpBinding.Type.PathType), None) =>
            // See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html?highlight=httpquery#httpquery-trait
            StringValueMetaEncode((timestamp: Timestamp) =>
              timestamp.format(smithy.api.TimestampFormat.DATE_TIME)
            )
          case (Some(HttpBinding.Type.HeaderType), None) =>
            // See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html?highlight=httpquery#httpheader-trait
            StringValueMetaEncode((timestamp: Timestamp) =>
              timestamp.format(smithy.api.TimestampFormat.HTTP_DATE)
            )
          case _ => MetaEncode.empty
        }
      case Primitive.PDocument => MetaEncode.empty[P]
      case Primitive.PUnit     => MetaEncode.empty[P]
      case _                   => fromToString[P]
    }
  }

  override def list[A](
      shapeId: ShapeId,
      hints: Hints,
      member: Schema[A]
  ): MetaEncode[List[A]] = {
    self(member) match {
      case StringValueMetaEncode(f) =>
        StringListMetaEncode[List[A]](listA => listA.map(f))
      case _ => MetaEncode.empty
    }
  }

  override def set[A](
      shapeId: ShapeId,
      hints: Hints,
      member: Schema[A]
  ): MetaEncode[Set[A]] = {
    self(member) match {
      case StringValueMetaEncode(f) =>
        StringListMetaEncode[Set[A]](set => set.map(f).toList)
      case _ => MetaEncode.empty
    }
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): MetaEncode[Map[K, V]] = {
    (self(key), self(value)) match {
      case (StringValueMetaEncode(keyF), StringValueMetaEncode(valueF)) =>
        StringMapMetaEncode(map =>
          map.map { case (k, v) =>
            (keyF(k), valueF(v))
          }
        )
      case (StringValueMetaEncode(keyF), StringListMetaEncode(valueF)) =>
        StringListMapMetaEncode(map =>
          map.map { case (k, v) =>
            (keyF(k), valueF(v))
          }
        )
      case _ => MetaEncode.empty
    }
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): MetaEncode[E] = {
    StringValueMetaEncode(e => total(e).stringValue)
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): MetaEncode[S] = {
    def encodeField[A](
        field: SchemaField[S, A]
    ): Option[(Metadata, S) => Metadata] = {
      val fieldHints = field.hints
      HttpBinding
        .fromHints(field.label, fieldHints, hints)
        .map { binding =>
          val folderT = new Field.LeftFolder[Schema, Metadata] {
            override def compile[T](
                label: String,
                instance: Schema[T]
            ): (Metadata, T) => Metadata = {
              val encoder: MetaEncode[T] =
                self(
                  instance.addHints(Hints(binding) ++ fieldHints)
                )
              val updateFunction = encoder.updateMetadata(binding)
              (metadata, t: T) => updateFunction(metadata, t)
            }
          }
          field.leftFolder(folderT)
        }
    }
    val updateFunctions = fields.flatMap(field => encodeField(field))

    StructureMetaEncode(s =>
      updateFunctions.foldLeft(Metadata.empty)((metadata, updateFunction) =>
        updateFunction(metadata, s)
      )
    )
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: U => SchemaAndValue[U, _]
  ): MetaEncode[U] = MetaEncode.empty

  override def biject[A, B](
      schema: Schema[A],
      to: A => B,
      from: B => A
  ): MetaEncode[B] = self(schema).contramap(from)

  override def surject[A, B](
      schema: Schema[A],
      to: Refinement[A, B],
      from: B => A
  ): MetaEncode[B] = self(schema).contramap(from)

  override def lazily[A](suspend: Lazy[Schema[A]]): MetaEncode[A] =
    MetaEncode.empty
}
