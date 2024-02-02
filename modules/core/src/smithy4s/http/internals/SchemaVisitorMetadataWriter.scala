/*
 *  Copyright 2021-2024 Disney Streaming
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

import smithy.api.HttpQueryParams
import smithy4s.http.HttpBinding
import smithy4s.http.internals.MetaEncode._
import smithy4s.schema.Alt
import smithy4s.schema.CollectionTag
import smithy4s.schema.CompilationCache
import smithy4s.schema.EnumTag
import smithy4s.schema.EnumValue
import smithy4s.schema.Field
import smithy4s.schema.Primitive
import smithy4s.schema.SchemaVisitor

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
class SchemaVisitorMetadataWriter(
    val cache: CompilationCache[MetaEncode],
    commaDelimitedEncoding: Boolean,
    explicitDefaultsEncoding: Boolean
) extends SchemaVisitor.Cached[MetaEncode] {
  self =>
  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): MetaEncode[P] = {
    val hasMedia = hints.has(smithy.api.MediaType)
    Primitive.stringWriter(tag, hints) match {
      case Some(write) if hasMedia =>
        StringValueMetaEncode(
          write andThen (str =>
            Base64.getEncoder().encodeToString(str.getBytes())
          )
        )
      case Some(write) => StringValueMetaEncode(write)
      case None        => MetaEncode.empty[P]
    }
  }

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): MetaEncode[C[A]] = {
    val amendedMember = member.addHints(httpHints(hints))
    SchemaVisitorHeaderMerge(amendedMember) match {
      case Some(toMergeableValue) if commaDelimitedEncoding =>
        StringValueMetaEncode[C[A]] { c =>
          tag.iterator(c).map(toMergeableValue).mkString(", ")
        }
      case _ =>
        self(amendedMember) match {
          case StringValueMetaEncode(f) =>
            StringListMetaEncode[C[A]](c => tag.iterator(c).map(f).toList)
          case _ => MetaEncode.empty
        }
    }

  }

  override def option[A](schema: Schema[A]): MetaEncode[Option[A]] =
    self(schema) match {
      case StringValueMetaEncode(f) =>
        StringValueMetaEncode {
          case Some(value) => f(value)
          case None        => ""
        }
      case StringListMetaEncode(f) =>
        StringListMetaEncode {
          case Some(value) => f(value)
          case None        => List.empty
        }
      case StringMapMetaEncode(f) =>
        StringMapMetaEncode {
          case Some(value) => f(value)
          case None        => Map.empty
        }
      case StringListMapMetaEncode(f) =>
        StringListMapMetaEncode {
          case Some(value) => f(value)
          case None        => Map.empty
        }
      case EmptyMetaEncode        => EmptyMetaEncode
      case StructureMetaEncode(_) => EmptyMetaEncode
    }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): MetaEncode[Map[K, V]] = {
    (self(key), self(value.addHints(httpHints(hints)))) match {
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
      tag: EnumTag[E],
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): MetaEncode[E] =
    tag match {
      case EnumTag.IntEnum() =>
        StringValueMetaEncode(e => total(e).intValue.toString())
      case _ =>
        StringValueMetaEncode(e => total(e).stringValue)
    }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): MetaEncode[S] = {
    def encodeField[A](
        field: Field[S, A]
    ): Option[(Metadata, S) => Metadata] = {
      val fieldHints = field.hints
      HttpBinding
        .fromHints(field.label, fieldHints, hints)
        .map { binding =>
          val encoder = self(field.schema.addHints(Hints(binding)))
          val updateFunction = encoder.updateMetadata(binding)
          (metadata, s) =>
            if (explicitDefaultsEncoding)
              field.get(s) match {
                case None => metadata
                case _    => updateFunction(metadata, field.get(s))
              }
            else
              field.getUnlessDefault(s) match {
                case Some(nonDefaultA) => updateFunction(metadata, nonDefaultA)
                case None              => metadata
              }
        }
    }
    // pull out the query params field as it must be applied last to the metadata
    val (queryParamFieldVec, theRest) =
      fields.partition(_.memberHints.has[HttpQueryParams])
    val queryParams =
      queryParamFieldVec.flatMap(field => encodeField(field)).headOption
    val updateFunctions = theRest.flatMap(field => encodeField(field))

    StructureMetaEncode(s => {
      //  this is a non commutative operation
      val metadata =
        updateFunctions.foldLeft(Metadata.empty)((metadata, updateFunction) =>
          updateFunction(metadata, s)
        )
      queryParams.fold(metadata)(updateFunction => updateFunction(metadata, s))
    })
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[Alt[U, _]],
      dispatcher: Alt.Dispatcher[U]
  ): MetaEncode[U] = MetaEncode.empty

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): MetaEncode[B] = self(schema).contramap(bijection.from)

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): MetaEncode[B] = self(schema).contramap(refinement.from)

  override def lazily[A](suspend: Lazy[Schema[A]]): MetaEncode[A] =
    MetaEncode.empty
}
