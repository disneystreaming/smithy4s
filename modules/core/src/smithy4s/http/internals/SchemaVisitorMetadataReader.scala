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
import smithy4s.internals.SchemaDescription
import scala.collection.immutable.ArraySeq

private[http] class SchemaVisitorMetadataReader(
    val cache: CompilationCache[MetaDecode]
) extends SchemaVisitor.Cached[MetaDecode]
    with ScalaCompat { self =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): MetaDecode[P] = {
    val desc = SchemaDescription.primitive(shapeId, hints, tag)
    tag match {
      case Primitive.PUnit =>
        MetaDecode.StructureMetaDecode(_ => Right(()))
      case other =>
        Primitive.stringParser(other, hints) match {
          case Some(parse) => MetaDecode.from(desc)(parse)
          case None        => MetaDecode.EmptyMetaDecode
        }
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
    if (hints.has[IntEnum]) {
      MetaDecode
        .from(
          s"Enum[${values.map(_.stringValue).mkString(",")}]"
        )(string =>
          values.find(v => string.toIntOption.contains(v.intValue)).map(_.value)
        )
    } else {
      MetaDecode
        .from(
          s"Enum[${values.map(_.stringValue).mkString(",")}]"
        )(string => values.find(_.stringValue == string).map(_.value))
    }
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
        field: SchemaField[S, A],
        index: Int
    ): Either[String, FieldDecode] = {
      val schema = field.instance
      val label = field.label
      val fieldHints = field.hints
      val maybeDefault = schema.getDefaultValue
      HttpBinding.fromHints(label, fieldHints, hints).map { binding =>
        val decoder: MetaDecode[_] =
          self(schema.addHints(Hints(binding)))
        val update = decoder
          .updateMetadata(
            binding,
            label,
            index,
            field.isOptional,
            reservedQueries,
            maybeDefault
          )
        FieldDecode(label, binding, update)
      } match {
        case None              => Left(label)
        case Some(fieldDecode) => Right(fieldDecode)
      }
    }
    val maybeUpdates: Either[String, Vector[FieldDecode]] =
      fields.zipWithIndex.traverse { case (field, index) =>
        decodeField(field, index)
      }

    maybeUpdates.map {
      fieldUpdates =>
        { (metadata: Metadata) =>
          val buffer = Array.fill[Any](fields.size)(null)
          val putField: PutField = new PutField {
            def putRequired(fieldIndex: Int, value: Any): Unit =
              buffer(fieldIndex) = value

            def putSome(fieldIndex: Int, value: Any): Unit =
              buffer(fieldIndex) = Some(value)

            def putNone(fieldIndex: Int): Unit =
              buffer(fieldIndex) = None
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
            Right(make(ArraySeq.unsafeWrapArray(buffer)))
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
    } match {
      case Left(fieldName) =>
        MetaDecode.ImpossibleMetaDecode(
          s"Field $fieldName is not bound to http metadata"
        )
      case Right(decode) => StructureMetaDecode(decode)
    }
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): MetaDecode[U] = EmptyMetaDecode

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): MetaDecode[B] = self(schema).map(bijection)

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): MetaDecode[B] = self(schema).map(refinement.asThrowingFunction)

  override def lazily[A](suspend: Lazy[Schema[A]]): MetaDecode[A] =
    EmptyMetaDecode
}
