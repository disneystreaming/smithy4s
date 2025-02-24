/*
 *  Copyright 2021-2025 Disney Streaming
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

package smithy4s.codecs

import smithy4s.schema.Field
import smithy4s.schema.Schema

trait FieldSkipCompiler { self =>
  def compile[A](
      field: Field[?, A]
  ): FieldSkipCompiler.ShouldRender[A]

  def combine(
      other: FieldSkipCompiler
  ): FieldSkipCompiler =
    new FieldSkipCompiler {

      def compile[A](
          field: Field[?, A]
      ): FieldSkipCompiler.ShouldRender[A] = {
        val r1 = self.compile(field)
        val r2 = other.compile(field)
        a => r1(a) && r2(a)
      }
    }
}

object FieldSkipCompiler {

  type ShouldRender[A] = A => Boolean

  private trait SkipNonRequired extends FieldSkipCompiler {

    final def compile[A](
        field: Field[?, A]
    ): FieldSkipCompiler.ShouldRender[A] = {
      if (field.isRequired) Function.const(true)
      else compileOptional(field)

    }

    def compileOptional[A](
        field: Field[?, A]
    ): FieldSkipCompiler.ShouldRender[A]

  }

  case object NeverSkip extends FieldSkipCompiler {
    def compile[A](field: Field[_, A]): ShouldRender[A] = Function.const(true)
  }

  private def asNonEmptyCollectionPredicate[F[_], A](
      schema: Schema[A]
  ): Option[A => Boolean] = {
    import Schema._
    schema match {
      case c: CollectionSchema[f, a] =>
        Some((collectionA: f[a]) => !c.tag.isEmpty(collectionA))
      case b: BijectionSchema[inner, a] =>
        asNonEmptyCollectionPredicate[F, inner](b.underlying)
          .map(_.compose(b.bijection.from))
      case r: RefinementSchema[inner, a] =>
        asNonEmptyCollectionPredicate[F, inner](r.underlying)
          .map(_.compose(r.refinement.from))
      case o: OptionSchema[inner] =>
        asNonEmptyCollectionPredicate(o.underlying)
          .map(predicateInner =>
            collectionA => collectionA.exists(predicateInner)
          )
      case _: MapSchema[k, v] =>
        Some(collectionA => collectionA.nonEmpty)
      case LazySchema(suspend) => asNonEmptyCollectionPredicate(suspend.value)
      case _: EnumerationSchema[_] => None
      case _: StructSchema[_]      => None
      case _: UnionSchema[_]       => None
      case _: PrimitiveSchema[_]   => None
    }
  }

  private case object skipIfEmptyOptionalCollection
      extends FieldSkipCompiler.SkipNonRequired {

    def compileOptional[A](field: Field[?, A]): ShouldRender[A] = {
      asNonEmptyCollectionPredicate(field.schema) match {
        case None             => Function.const(true)
        case Some(isNonEmpty) => isNonEmpty
      }
    }
  }

  val SkipIfEmptyOptionalCollection: FieldSkipCompiler =
    skipIfEmptyOptionalCollection

  case object SkipIfEmptyCollection extends FieldSkipCompiler {

    def compile[A](field: Field[_, A]): ShouldRender[A] = {
      asNonEmptyCollectionPredicate(field.schema) match {
        case None             => Function.const(true)
        case Some(isNonEmpty) => isNonEmpty
      }
    }
  }

  private case object skipIfDefaultOptionals
      extends FieldSkipCompiler.SkipNonRequired {
    def compileOptional[A](field: Field[?, A]): ShouldRender[A] = {
      // Optional fields have None as their default, so we need to make sure not to skip them here
      a => a == None || !field.isDefaultValue(a)
    }
  }

  val SkipIfDefaultOptionals: FieldSkipCompiler = skipIfDefaultOptionals

  private case object skipIfEmptyOptionals
      extends FieldSkipCompiler.SkipNonRequired {
    def compileOptional[A](field: Field[?, A]): ShouldRender[A] = { a =>
      a != None
    }
  }

  val SkipIfEmptyOptionals: FieldSkipCompiler = skipIfEmptyOptionals

  object SkipIfEmptyOrDefaultOptionals extends FieldSkipCompiler {
    def compile[A](field: Field[_, A]): ShouldRender[A] =
      (SkipIfEmptyOptionals combine SkipIfDefaultOptionals).compile(field)
  }
}
