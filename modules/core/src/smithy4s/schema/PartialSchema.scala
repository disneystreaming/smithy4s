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

import smithy4s.PartialData
import smithy4s.kinds.PolyFunction
import Schema._

/**
  * A structure indicating the match result of running `Schema#partial` against a given predicate
  *
  *   - if the schema is not of a structure, or if none of the fields matched, then `NoMatch` should be returned
  *   - if the schema is a structure and only a subset of its fields pass the predicate, then `PartialMatch` should be returned
  *   - if the schema is a structure and all of its fields pass the predicate, then `TotalMatch` should be returned
  */
sealed trait PartialSchema[A]

object PartialSchema {

  // format: off
  final case class TotalMatch[A](schema: Schema[A])                extends PartialSchema[A]
  final case class PartialMatch[A](schema: Schema[PartialData[A]]) extends PartialSchema[A]
  final case class NoMatch[A]()                                    extends PartialSchema[A]
  // format: on

  private[schema] def apply(
      keep: SchemaField[_, _] => Boolean,
      payload: Boolean
  ): PolyFunction[Schema, PartialSchema] =
    new PolyFunction[Schema, PartialSchema] {

      def apply[S](fa: Schema[S]): PartialSchema[S] = {
        fa match {
          case StructSchema(shapeId, hints, fields, make) =>
            if (payload) {
              fields.zipWithIndex
                .find { case (schemaField, _) =>
                  keep(schemaField)
                }
                .map { case (allowedField, index) =>
                  allowedField.fold(
                    bijectSingle(index, make, total = fields.size == 1)
                  )
                }
                .getOrElse {
                  PartialSchema.NoMatch()
                }
            } else {
              val allowedFields = fields.zipWithIndex.filter {
                case (schemaField, _) => keep(schemaField)
              }
              if (allowedFields.size == 0) {
                PartialSchema.NoMatch()
              } else if (allowedFields.size == fields.size) {
                PartialSchema.TotalMatch(fa)
              } else {
                val indexes = allowedFields.map(_._2)
                val unsafeAccessFields = allowedFields.map {
                  case (schemaField, _) =>
                    schemaField.foldK(fieldFolder[S])
                }
                def const(values: IndexedSeq[Any]): PartialData[S] =
                  PartialData.Partial(indexes, values, make)
                PartialSchema.PartialMatch(
                  StructSchema(shapeId, hints, unsafeAccessFields, const)
                )
              }
            }
          case BijectionSchema(underlying, bijection) =>
            apply(underlying) match {
              case PartialSchema.PartialMatch(partial) =>
                PartialSchema.PartialMatch(
                  partial.biject(_.map(bijection.to), _.map(bijection.from))
                )
              case PartialSchema.TotalMatch(total) =>
                PartialSchema.TotalMatch(total.biject(bijection))
              case PartialSchema.NoMatch() => PartialSchema.NoMatch()
            }
          case LazySchema(s) => apply(s.value)
          case _             => PartialSchema.NoMatch()
        }
      }
    }

  // When a single field is assumed to contribute to a whole "payload"
  // (like, an http-body), a couple things can happen :
  //
  // * either that is the only field in a structure, therefore the decoding
  //   of that structure equate to the decoding of whatever type the field
  //   holds and its wrapping in the structure's instance
  // * or the decoding of the type the field holds gives a value that needs
  //   to be reconciled with others, therefore a `PartialData.Partial` is needed
  //
  // We can use a bijection to express either things. When the payload is only
  // part of the larger data, that bijection makes use of the getter
  private def bijectSingle[S](
      index: Int,
      make: IndexedSeq[Any] => S,
      total: Boolean
  ) =
    new Field.Folder[Schema, S, PartialSchema[S]] {
      def onRequired[A](
          label: String,
          instance: Schema[A],
          get: S => A
      ): PartialSchema[S] = if (total) {
        val to = (a: A) => make(IndexedSeq(a))
        val from = get
        PartialSchema.TotalMatch(instance.biject(to, from))
      } else {
        val indexes = IndexedSeq(index)
        val to = (a: A) => PartialData.Partial(indexes, IndexedSeq(a), make)
        val from = (_: PartialData[S]) match {
          case PartialData.Total(s) => get(s)
          case _                    => codingError
        }
        PartialSchema.PartialMatch(instance.biject(to, from))
      }
      def onOptional[A](
          label: String,
          instance: Schema[A],
          get: S => Option[A]
      ): PartialSchema[S] = PartialSchema.NoMatch()
    }

  private def codingError: Nothing =
    sys.error("Coding error: this should not happen on encoding side")

  private def fieldFolder[S] =
    new Field.FolderK[Schema, S, SchemaField[PartialData[S], *]] {
      def onRequired[A](
          label: String,
          instance: Schema[A],
          get: S => A
      ): SchemaField[PartialData[S], A] = {
        def access(product: PartialData[S]): A = product match {
          case PartialData.Total(struct)    => get(struct)
          case PartialData.Partial(_, _, _) => codingError
        }
        instance.required(label, access)
      }
      def onOptional[A](
          label: String,
          instance: Schema[A],
          get: S => Option[A]
      ): SchemaField[PartialData[S], Option[A]] = {
        def access(product: PartialData[S]): Option[A] = product match {
          case PartialData.Total(struct)    => get(struct)
          case PartialData.Partial(_, _, _) => codingError
        }
        instance.optional(label, access)
      }
    }

}
