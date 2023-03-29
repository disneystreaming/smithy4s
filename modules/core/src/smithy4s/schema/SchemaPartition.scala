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
  * A structure indicating the match result of running `Schema#partition` against a given predicate
  *
  *   - if the schema is not of a structure, or if none of the fields matched, then `NoMatch` should be returned
  *   - if the schema is a structure and only a subset of its fields pass the predicate, then `PartialMatch` should be returned
  *   - if the schema is a structure and all of its fields pass the predicate, then `TotalMatch` should be returned
  */
sealed trait SchemaPartition[A]

object SchemaPartition {

  // format: off
  final case class TotalMatch[A](schema: Schema[A])                                                         extends SchemaPartition[A]
  final case class SplittingMatch[A](matching: Schema[PartialData[A]], notMatching: Schema[PartialData[A]]) extends SchemaPartition[A]
  final case class NoMatch[A]()                                                                             extends SchemaPartition[A]
  // format: on

  private[schema] def apply(
      keep: SchemaField[_, _] => Boolean,
      payload: Boolean
  ): PolyFunction[Schema, SchemaPartition] =
    new PolyFunction[Schema, SchemaPartition] {

      def apply[S](fa: Schema[S]): SchemaPartition[S] = {
        fa match {
          case StructSchema(shapeId, hints, fields, make) =>
            def buildPartialDataSchema(
                fieldsAndIndexes: Vector[(SchemaField[S, _], Int)]
            ): Schema[PartialData[S]] = {
              val indexes = fieldsAndIndexes.map(_._2)
              val unsafeAccessField = fieldsAndIndexes.map {
                case (schemaField, _) =>
                  schemaField.foldK(fieldFolder[S])
              }
              def const(values: IndexedSeq[Any]): PartialData[S] =
                PartialData.Partial(indexes, values, make)

              StructSchema(shapeId, hints, unsafeAccessField, const)
            }

            if (payload) {
              fields.zipWithIndex
                .find { case (schemaField, _) =>
                  keep(schemaField)
                }
                .map { case (allowedField, index) =>
                  val notMatchingFields =
                    fields.zipWithIndex.filterNot(_._2 == index)
                  val maybeNotMatchingSchema = if (notMatchingFields.size > 0) {
                    Some(buildPartialDataSchema(notMatchingFields))
                  } else None
                  allowedField.fold(
                    bijectSingle(index, make, maybeNotMatchingSchema)
                  )
                }
                .getOrElse {
                  SchemaPartition.NoMatch()
                }
            } else {
              val (matchingFields, notMatchingFields) =
                fields.zipWithIndex.partition { case (schemaField, _) =>
                  keep(schemaField)
                }
              if (matchingFields.size == 0) {
                SchemaPartition.NoMatch()
              } else if (matchingFields.size == fields.size) {
                SchemaPartition.TotalMatch(fa)
              } else {
                SchemaPartition.SplittingMatch(
                  buildPartialDataSchema(matchingFields),
                  buildPartialDataSchema(notMatchingFields)
                )
              }
            }
          case BijectionSchema(underlying, bijection) =>
            apply(underlying) match {
              case SchemaPartition.SplittingMatch(matching, notMatching) =>
                SchemaPartition.SplittingMatch(
                  matching.biject(_.map(bijection.to), _.map(bijection.from)),
                  notMatching.biject(_.map(bijection.to), _.map(bijection.from))
                )
              case SchemaPartition.TotalMatch(total) =>
                SchemaPartition.TotalMatch(total.biject(bijection))
              case SchemaPartition.NoMatch() => SchemaPartition.NoMatch()
            }
          case LazySchema(s) => apply(s.value)
          case _             => SchemaPartition.NoMatch()
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
      maybeNotMatching: Option[Schema[PartialData[S]]]
  ) =
    new Field.Folder[Schema, S, SchemaPartition[S]] {
      def onRequired[A](
          label: String,
          instance: Schema[A],
          get: S => A
      ): SchemaPartition[S] =
        maybeNotMatching match {
          case None =>
            // The payload field is the only field and we can create a total
            // match from it, by bijecting from its result onto the structure
            val to = (a: A) => make(IndexedSeq(a))
            val from = get
            SchemaPartition.TotalMatch(instance.biject(to, from))
          case Some(notMachingSchema) =>
            // There are other fields than the payload field.
            val indexes = IndexedSeq(index)
            val to = (a: A) => PartialData.Partial(indexes, IndexedSeq(a), make)
            val from = (_: PartialData[S]) match {
              case PartialData.Total(s) => get(s)
              case _                    => codingError
            }
            SchemaPartition.SplittingMatch(
              instance.biject(to, from),
              notMachingSchema
            )
        }
      def onOptional[A](
          label: String,
          instance: Schema[A],
          get: S => Option[A]
      ): SchemaPartition[S] = SchemaPartition.NoMatch()
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
