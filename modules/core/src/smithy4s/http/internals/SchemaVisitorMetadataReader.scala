/*
 *  Copyright 2021-2023 Disney Streaming
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

import java.util.Base64

/**
  * SchemaVisitor that implements the decoding of smithy4s.http.Metadata, which
  * contains values such as path-parameters, query-parameters, headers, and status code.
  *
  * @param awsHeaderEncoding defines whether the AWS encoding of headers should be expected.
  */
private[http] class SchemaVisitorMetadataReader(
    val cache: CompilationCache[MetaDecode],
    awsHeaderEncoding: Boolean
) extends SchemaVisitor.Cached[MetaDecode]
    with ScalaCompat { self =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): MetaDecode[P] = {
    val desc = SchemaDescription.primitive(shapeId, hints, tag)
    val hasMedia = hints.has(smithy.api.MediaType)
    Primitive.stringParser(tag, hints) match {
      case Some(parse) if hasMedia =>
        MetaDecode.from(desc)(
          parse.compose[String](str =>
            new String(Base64.getDecoder().decode(str))
          )
        )
      case Some(parse) => MetaDecode.from(desc)(parse)
      case None        => MetaDecode.EmptyMetaDecode
    }
  }

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): MetaDecode[C[A]] = {
    val amendedMember = member.addHints(httpHints(hints))
    self(amendedMember) match {
      case MetaDecode.StringValueMetaDecode(f) =>
        val isAwsHeader = hints
          .get(HttpBinding)
          .exists(_.tpe == HttpBinding.Type.HeaderType) && awsHeaderEncoding
        (SchemaVisitorHeaderSplit(member), isAwsHeader) match {
          case (Some(splitFunction), true) =>
            MetaDecode.StringCollectionMetaDecode[C[A]] { it =>
              tag.fromIterator(it.flatMap(splitFunction).map(f))
            }
          case (_, _) =>
            MetaDecode.StringCollectionMetaDecode[C[A]] { it =>
              tag.fromIterator(it.map(f))
            }
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
    (self(key), self(value.addHints(httpHints(hints)))) match {
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
      tag: EnumTag[E],
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): MetaDecode[E] = {
    val intVals = s"Enum[${values.map(_.stringValue).mkString(",")}]"
    val stringVals = s"Enum[${values.map(_.stringValue).mkString(",")}]"
    val handleInt: Option[Int] => Option[E] = { maybeInt =>
      values
        .find(v => maybeInt.contains(v.intValue))
        .map(_.value)
    }
    val handleString: String => Option[E] = { string =>
      values.find(_.stringValue == string).map(_.value)
    }
    tag match {
      case EnumTag.ClosedIntEnum =>
        MetaDecode.from(intVals)(str => handleInt(str.toIntOption))
      case EnumTag.OpenIntEnum(unknown) =>
        MetaDecode.from(intVals) { string =>
          val maybeInt = string.toIntOption
          handleInt(maybeInt).orElse(maybeInt.map(unknown))
        }
      case EnumTag.ClosedStringEnum =>
        MetaDecode.from(stringVals)(handleString)
      case EnumTag.OpenStringEnum(unknown) =>
        MetaDecode.from(stringVals)(str =>
          Some(handleString(str).getOrElse(unknown(str)))
        )
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
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): MetaDecode[S] = {

    def decodeField[A](
        field: Field[S, A]
    ): Option[FieldDecode] = {
      val schema = field.schema
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
            maybeDefault
          )
        FieldDecode(label, binding, update)
      }
    }
    val fieldUpdates: Vector[FieldDecode] =
      fields.flatMap(f => decodeField(f))

    if (fieldUpdates.size < fields.size) EmptyMetaDecode
    else
      StructureMetaDecode { (metadata: Metadata) =>
        val buffer = Vector.newBuilder[Any]
        val putField: PutField = buffer += _

        var currentFieldName: String = null
        var currentBinding: HttpBinding = null
        try {
          fieldUpdates.foreach { case FieldDecode(fieldName, binding, update) =>
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
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[Alt[U, _]],
      dispatch: Alt.Dispatcher[U]
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

  override def option[A](schema: Schema[A]): MetaDecode[Option[A]] =
    self(schema).map(Some(_))
}
