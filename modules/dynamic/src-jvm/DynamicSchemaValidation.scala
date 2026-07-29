/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s.dynamic

import smithy4s.Document
import smithy4s.RefinementProvider
import smithy4s.ShapeId
import smithy4s.schema.Primitive._
import smithy4s.schema.Schema
import smithy4s.schema.Schema._
import smithy4s.~>

/**
  * Dynamically-loaded schemas only carry constraint traits (`@length`, `@range`,
  * `@pattern` etc) as hints: by default, `DynamicModelCompiler` only attaches them via
  * `addHints`, unlike smithy4s codegen, which reifies such traits into `RefinementSchema`
  * wrappers that get enforced upon decoding.
  *
  * This object provides a transformation for dynamically-loaded schemas, so that validation 
  * hints are reintroduced at a `Schema` level.
  */
private[dynamic] object DynamicSchemaValidation {

  def reifyConstraints(index: DynamicSchemaIndex): DynamicSchemaIndex =
    new DynamicSchemaIndex {
      private val schemaMap: Map[ShapeId, Schema[_]] =
        index.allSchemas.map(s => s.shapeId -> reifySchema(s)).toMap
      def allServices: Iterable[DynamicSchemaIndex.ServiceWrapper] =
        index.allServices
      def allSchemas: Iterable[Schema[_]] =
        schemaMap.values
      def getSchema(shapeId: ShapeId): Option[Schema[_]] =
        schemaMap.get(shapeId)
      def metadata: Map[String, Document] = index.metadata
    }

  private def reifySchema[A](schema: Schema[A]): Schema[A] =
    schema.transformTransitivelyK(ReifyConstraints)

  private object ReifyConstraints extends (Schema ~> Schema) {

    private implicit class SchemaOps[A](schema: Schema[A]) {
      def reifyHint[B](rp: RefinementProvider[B, A, ?]): Schema[A] =
        schema.hints
          .get(rp.tag)
          .fold(schema)(schema.validated(_)(RefinementProvider.void(rp)))
    }

    private def collection[C[_], B](
        schema: Schema.CollectionSchema[C, B]
    ): Schema[C[B]] =
      schema.reifyHint(
        RefinementProvider.lengthConstraint[C[B]](schema.tag.iterator(_).size)
      )

    private def enumSchema[B <: Enum[?]](
        schema: Schema.EnumerationSchema[B]
    ): Schema[B] =
      schema
        .reifyHint(RefinementProvider.lengthConstraint[B](_.toString.length))
        .reifyHint(RefinementProvider.rangeConstraint[B, Int](_.ordinal()))
        .reifyHint(RefinementProvider.patternConstraint[B](e => e.toString))

    def apply[A](schema: Schema[A]): Schema[A] =
      schema match {
        case t @ PrimitiveSchema(_, _, tag) =>
          tag match {
            case PString =>
              t.reifyHint(RefinementProvider.stringLengthConstraint)
                .reifyHint(RefinementProvider.stringPatternConstraints)
                .reifyHint(RefinementProvider.idRefRefinement)
            case PByte =>
              schema.reifyHint(RefinementProvider.numericRangeConstraints[Byte])
            case PShort =>
              schema.reifyHint(
                RefinementProvider.numericRangeConstraints[Short]
              )
            case PInt =>
              schema.reifyHint(RefinementProvider.numericRangeConstraints[Int])
            case PLong =>
              schema.reifyHint(RefinementProvider.numericRangeConstraints[Long])
            case PFloat =>
              schema.reifyHint(
                RefinementProvider.numericRangeConstraints[Float]
              )
            case PDouble =>
              schema.reifyHint(
                RefinementProvider.numericRangeConstraints[Double]
              )
            case PBigInt =>
              schema.reifyHint(
                RefinementProvider.numericRangeConstraints[BigInt]
              )
            case PBigDecimal =>
              schema.reifyHint(
                RefinementProvider.numericRangeConstraints[BigDecimal]
              )
            case PBlob =>
              schema.reifyHint(RefinementProvider.blobLengthConstraint)
            case PTimestamp | PDocument | PBoolean | PUUID | PLocalDate |
                PLocalTime | PDuration | POffsetDateTime =>
              schema
          }
        case e: EnumerationSchema[?] =>
          enumSchema(e.asInstanceOf[EnumerationSchema[Enum[?]]])
            .asInstanceOf[Schema[A]]
        case c @ CollectionSchema(_, _, _, _) => collection(c)
        case m: MapSchema[c, k, v] =>
          m.reifyHint(
            RefinementProvider.lengthConstraint[c[k, v]](m.tag.iterator(_).size)
          )
        // explicitly handling each remaining case, in order to get a "missing match" warning if the schema model changes
        case b: BijectionSchema[_, _]  => b
        case r: RefinementSchema[_, _] => r
        case s: StructSchema[_]        => s
        case l: LazySchema[_]          => l
        case u: UnionSchema[_]         => u
        case n: OptionSchema[_, _]     => n
      }
  }

}
