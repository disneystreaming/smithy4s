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

package smithy4s.schema

import alloy.Nullable
import smithy4s.schema.Schema.BijectionSchema
import smithy4s.schema.Schema.LazySchema
import smithy4s.schema.Schema.OptionSchema
import smithy4s.schema.Schema.RefinementSchema
import smithy4s.~>

trait FieldFilter { self =>
  def compile[S, A](
      field: Field[S, A]
  ): FieldFilter.Predicate[A]

  def &&(other: FieldFilter): FieldFilter =
    FieldFilter.FieldFilterAnd(this, other)

  def ||(other: FieldFilter): FieldFilter =
    FieldFilter.FieldFilterOr(this, other)

  def !(): FieldFilter = FieldFilter.FieldFilterNot(this)

}

object FieldFilter {

  type Predicate[A] = A => Boolean

  private case class FieldFilterAnd(left: FieldFilter, right: FieldFilter)
      extends FieldFilter {

    def compile[S, A](
        field: Field[S, A]
    ): FieldFilter.Predicate[A] = {
      val r1 = left.compile(field)
      val r2 = right.compile(field)
      a => r1(a) && r2(a)
    }
  }

  private case class FieldFilterOr(left: FieldFilter, right: FieldFilter)
      extends FieldFilter {

    def compile[S, A](
        field: Field[S, A]
    ): FieldFilter.Predicate[A] = {
      val r1 = left.compile(field)
      val r2 = right.compile(field)
      a => r1(a) || r2(a)
    }
  }

  private case class FieldFilterNot(inner: FieldFilter) extends FieldFilter {

    def compile[S, A](
        field: Field[S, A]
    ): FieldFilter.Predicate[A] = {
      val r1 = inner.compile(field)
      a => !r1(a)
    }
  }

  private trait SkipNonRequired extends FieldFilter {

    final def compile[S, A](
        field: Field[S, A]
    ): FieldFilter.Predicate[A] = {
      if (field.isRequired) Function.const(true)
      else compileNonRequired(field)

    }

    def compileNonRequired[S, A](
        field: Field[S, A]
    ): FieldFilter.Predicate[A]

  }

  case object EncodeAll extends FieldFilter {
    def compile[S, A](field: Field[S, A]): Predicate[A] = Function.const(true)
  }

  private def asNonEmptyCollectionPredicate[A](
      schema: Schema[A]
  ): Option[A => Boolean] = {
    import Schema._
    schema match {
      case c: CollectionSchema[f, a] =>
        Some((collectionA: f[a]) => !c.tag.isEmpty(collectionA))
      case b: BijectionSchema[inner, a] =>
        asNonEmptyCollectionPredicate[inner](b.underlying)
          .map(_.compose(b.bijection.from))
      case r: RefinementSchema[inner, a] =>
        asNonEmptyCollectionPredicate[inner](r.underlying)
          .map(_.compose(r.refinement.from))
      case o: OptionSchema[f, inner] =>
        asNonEmptyCollectionPredicate(o.underlying)
          .map(predicateInner =>
            (collectionA: f[inner]) => o.tag.exists(collectionA, predicateInner)
          )
      case c: MapSchema[c, k, v] =>
        Some((collectionA: c[k, v]) => !c.tag.isEmpty(collectionA))
      case LazySchema(suspend) =>
        // it is safe to call .value here because we don't recurse into structs/unions schemas,
        // so we never see the same schema twice in this visitor.
        asNonEmptyCollectionPredicate(suspend.value)
      case _: EnumerationSchema[_] => None
      case _: StructSchema[_]      => None
      case _: UnionSchema[_]       => None
      case _: PrimitiveSchema[_]   => None
    }
  }

  private case object skipEmptyOptionalCollection
      extends FieldFilter.SkipNonRequired {

    def compileNonRequired[S, A](field: Field[S, A]): Predicate[A] = {
      asNonEmptyCollectionPredicate(field.schema) match {
        case None             => Function.const(true)
        case Some(isNonEmpty) => isNonEmpty
      }
    }
  }

  val SkipEmptyOptionalCollection: FieldFilter =
    skipEmptyOptionalCollection

  case object SkipEmptyCollection extends FieldFilter {

    def compile[S, A](field: Field[S, A]): Predicate[A] = {
      asNonEmptyCollectionPredicate(field.schema) match {
        case None             => Function.const(true)
        case Some(isNonEmpty) => isNonEmpty
      }
    }
  }

  private case object skipNonRequiredDefaultValues
      extends FieldFilter.SkipNonRequired {
    def compileNonRequired[S, A](field: Field[S, A]): Predicate[A] = {
      // do not use field.hasDefaultValue as it returns always true for options
      if (field.schema.getDefault.isDefined) { a =>
        !field.isDefaultValue(a)
      } else {
        Function.const(true)
      }
    }
  }

  val SkipNonRequiredDefaultValues: FieldFilter = skipNonRequiredDefaultValues

  private object IsNoneVisitor extends (Schema ~> Predicate) {

    def apply[A](schema: Schema[A]): Predicate[A] = schema match {
      // nullables are technically never None, so we fall through
      case o: OptionSchema[f, inner] =>
        if (o.underlying.hints.has(Nullable) && !o.underlying.isOption)
          Function.const(false)
        else
          (v: f[inner]) => o.tag.isNone(v)

      case BijectionSchema(underlying, bijection) =>
        this(underlying).compose(bijection.from)

      case RefinementSchema(underlying, refinement) =>
        this(underlying).compose(refinement.from)

      // technically, realistically this case is probably not reachable
      // because a recursive schema be wrapped in an option first, and wouldn't be traversed by this visitor.
      // could possibly be reached from a recursive list/map
      case LazySchema(suspend) =>
        val underlying = suspend.map(this(_))
        v => underlying.value(v)

      case _ => Function.const(false)
    }
  }

  private case object skipUnsetOptions extends FieldFilter.SkipNonRequired {
    def compileNonRequired[S, A](field: Field[S, A]): Predicate[A] =
      IsNoneVisitor(field.schema).andThen(!_)
  }

  val SkipUnsetOptions: FieldFilter = skipUnsetOptions

  val Default = SkipUnsetOptions && SkipNonRequiredDefaultValues
}
