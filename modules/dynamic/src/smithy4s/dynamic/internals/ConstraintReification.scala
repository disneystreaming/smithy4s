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

package smithy4s.dynamic.internals

import smithy4s.RefinementProvider
import smithy4s.schema.Primitive._
import smithy4s.schema.Schema
import smithy4s.schema.Schema._
import smithy4s.~>

/**
  * Dynamically-loaded schemas only carry constraint traits (`@length`, `@range`,
  * `@pattern` etc) as hints: `DynamicModelCompiler` only attaches them via `addHints`/
  * `addMemberHints`, unlike smithy4s codegen, which reifies such traits into
  * `RefinementSchema` wrappers that get enforced upon decoding.
  *
  * This is a single-node (non-recursive) transform: it only inspects the hints already
  * present on the schema it's given. `DynamicModelCompiler` applies it locally, at each
  * point a schema's hints have just finished being merged (a shape's own top-level hints,
  * or a member/field's merged hints) and are about to be handed to a consumer — composition
  * across nesting levels then falls out for free, since a nested schema is already reified
  * by the time its parent embeds it.
  */
private[dynamic] object ConstraintReification extends (Schema ~> Schema) {

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
            schema.reifyHint(RefinementProvider.numericRangeConstraints[Short])
          case PInt =>
            schema.reifyHint(RefinementProvider.numericRangeConstraints[Int])
          case PLong =>
            schema.reifyHint(RefinementProvider.numericRangeConstraints[Long])
          case PFloat =>
            schema.reifyHint(RefinementProvider.numericRangeConstraints[Float])
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
      case e: EnumerationSchema[_] =>
        val byValue = e.values.map(v => v.value -> v).toMap
        schema
          .reifyHint(
            RefinementProvider.lengthConstraint[A](
              byValue(_).stringValue.length
            )
          )
          .reifyHint(
            RefinementProvider.rangeConstraint[A, Int](byValue(_).intValue)
          )
          .reifyHint(
            RefinementProvider.patternConstraint[A](byValue(_).stringValue)
          )
      case c @ CollectionSchema(_, _, _, _) => collection(c)
      case m: MapSchema[c, k, v] =>
        m.reifyHint(
          RefinementProvider.lengthConstraint[c[k, v]](m.tag.iterator(_).size)
        )
      // We need to unpack the underlying schema and apply all the hints turning them into refinements.
      // This is because, for example in the container context we might have member level hints.
      // For example, if we have a list that targets constrained type and adds another constraint on member level
      // if we just retrieved a target member it would miss the member level constraint needed for the container case.
      case r: RefinementSchema[_, _] =>
        // Only used in dynamic context, A is fixed to DynData = Any
        apply(r.underlying).asInstanceOf[Schema[A]]
      case b: BijectionSchema[_, _] => b
      case s: StructSchema[_]       => s
      case l: LazySchema[_]         => l
      case u: UnionSchema[_]        => u
      case n: OptionSchema[_, _]    => n
    }

}
