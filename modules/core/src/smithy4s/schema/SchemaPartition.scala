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
  /**
    * Indicates that all fields of a schema matched a condition.
    *
    * @param schema The schema resulting from the total match might not be the same as the input-schema:
    * if the partition aimed at finding a payload field, and if the whole data can be constructed from a
    * single payload field, the resulting schema would be a bijection from that payload field to the larger
    * datatype.
    */
  final case class TotalMatch[A](schema: Schema[A]) extends SchemaPartition[A]

  /**
    * Indicates that only a subset of fields matched the partitioning condition. This  datatype contains
    * two schemas representing the partial data resulting from the partitioning. For instance :
    * http-header fields and non-http-header fields.
    *
    * The schemas can be dispatched to the correct SchemaVisitors to produce the relevant codecs. The
    * partial-data produced by either parts can be reconciled to create the total data.
    *
    * @param matching the partial schema resulting from the matching fields
    * @param notMatching the partial schema resulting from the non-matching fields
    */
  final case class SplittingMatch[A](matching: Schema[PartialData[A]], notMatching: Schema[PartialData[A]]) extends SchemaPartition[A]

  /**
    * Indicates that no field matched the condition.
    */
  final case class NoMatch[A]() extends SchemaPartition[A]
  // format: on

  private[schema] def apply(
      keep: Field[_, _] => Boolean,
      payload: Boolean
  ): PolyFunction[Schema, SchemaPartition] =
    new PolyFunction[Schema, SchemaPartition] {

      def apply[S](fa: Schema[S]): SchemaPartition[S] = {
        fa match {
          case StructSchema(shapeId, hints, fields, make) =>
            def buildPartialDataSchema(
                fieldsAndIndexes: Vector[(Field[S, _], Int)]
            ): Schema[PartialData[S]] = {
              val indexes = fieldsAndIndexes.map(_._2)
              val unsafeAccessFields = fieldsAndIndexes.map {
                case (schemaField, _) =>
                  toPartialDataField(schemaField)
              }
              def const(values: IndexedSeq[Any]): PartialData[S] =
                PartialData.Partial(indexes, values, make)

              StructSchema(shapeId, hints, unsafeAccessFields, const)
            }

            if (payload) {
              fields.zipWithIndex
                .find { case (schemaField, _) => keep(schemaField) }
                .map { case (allowedField, index) =>
                  val remainingFields =
                    fields.zipWithIndex.filterNot(_._2 == index)

                  val maybeRemainingSchema =
                    if (remainingFields.isEmpty) None
                    else Some(buildPartialDataSchema(remainingFields))

                  bijectSingle(allowedField, index, make, maybeRemainingSchema)
                }
                .getOrElse {
                  SchemaPartition.NoMatch()
                }
            } else {
              val partitioned = fields.zipWithIndex.partition {
                case (schemaField, _) => keep(schemaField)
              }

              partitioned match {
                case (matched, _) if matched.isEmpty =>
                  SchemaPartition.NoMatch()

                case (_, remaining) if remaining.isEmpty =>
                  SchemaPartition.TotalMatch(fa)

                case (matchingFields, remainingFields) =>
                  SchemaPartition.SplittingMatch(
                    buildPartialDataSchema(matchingFields),
                    buildPartialDataSchema(remainingFields)
                  )
              }
            }

          case BijectionSchema(underlying, bijection) =>
            apply(underlying) match {
              case SchemaPartition.SplittingMatch(matching, notMatching) =>
                SchemaPartition.SplittingMatch(
                  matching.biject(_.map(bijection.to))(_.map(bijection.from)),
                  notMatching.biject(_.map(bijection.to))(_.map(bijection.from))
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
  private def bijectSingle[S, A](
      field: Field[S, A],
      index: Int,
      make: IndexedSeq[Any] => S,
      maybeNotMatching: Option[Schema[PartialData[S]]]
  ): SchemaPartition[S] = {
    maybeNotMatching match {
      case None =>
        // The payload field is the only field and we can create a total
        // match from it, by bijecting from its result onto the structure
        val to = (a: A) => make(IndexedSeq(a))
        val from = field.get
        SchemaPartition.TotalMatch(field.schema.biject(to)(from))

      case Some(notMachingSchema) =>
        // There are other fields in the structure than the payload field.

        val to: A => PartialData[S] = {
          val indexes = IndexedSeq(index)
          (a: A) => PartialData.Partial(indexes, IndexedSeq(a), make)
        }

        val from = (_: PartialData[S]) match {
          case PartialData.Total(s)      => field.get(s)
          case _: PartialData.Partial[_] =>
            // It's impossible to get the whole struct from a single field if it's not the only one
            codingError
        }

        SchemaPartition.SplittingMatch(
          field.schema.biject(to)(from),
          notMachingSchema
        )
    }
  }

  private def codingError: Nothing =
    sys.error("Coding error: this should not happen on the encoding side")

  private def toPartialDataField[S, A](
      field: Field[S, A]
  ): Field[PartialData[S], A] = {
    def access(product: PartialData[S]): A = product match {
      case PartialData.Total(struct)    => field.get(struct)
      case PartialData.Partial(_, _, _) => codingError
    }
    Field(field.label, field.schema, access)
  }

}
