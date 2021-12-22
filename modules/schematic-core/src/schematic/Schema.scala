/*
 *  Copyright 2021 Disney Streaming
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

package schematic

/**
  * Purposefully open interface meant to enable building schemas as values,
  * in an extensible manner.
  *
  * The metamodel gets inferred automatically via contravariance.
  */
trait Schema[-S[_[_]], A] {

  import Schema._

  def compile[F[_]](s: S[F]): F[A]
  final def apply[F[_]](s: S[F]): F[A] = compile(s)
  @deprecated("use compile instead")
  final def form[F[_]](s: S[F]): F[A] = compile(s)

  final def required[Struct]: RequiredPartiallyApplied[S, Struct, A] =
    new RequiredPartiallyApplied[S, Struct, A](this)

  final def optional[Struct]: OptionalPartiallyApplied[S, Struct, A] =
    new OptionalPartiallyApplied[S, Struct, A](this)

  final def oneOf[Union]: OneOfPartiallyApplied[S, Union, A] =
    new OneOfPartiallyApplied[S, Union, A](this)

}

object Schema {
  type stdlib[A] = Schema[Schematic.stdlib, A]

  final class RequiredPartiallyApplied[-S[_[_]], Struct, A](
      val schema: Schema[S, A]
  ) {
    def apply[H](
        label: String,
        accessor: Struct => A
    ): StructureField[S, Struct, A] =
      RequiredField(label, schema, accessor)
  }

  final class OptionalPartiallyApplied[-S[_[_]], Struct, A](
      val schema: Schema[S, A]
  ) {
    def apply[H](
        label: String,
        accessor: Struct => Option[A]
    ): StructureField[S, Struct, Option[A]] =
      OptionalField(label, schema, accessor)
  }

  final class OneOfPartiallyApplied[-S[_[_]], Union, A](
      val schema: Schema[S, A]
  ) {
    def apply[H](label: String, inject: A => Union): OneOf[S, Union, A] =
      OneOf(label, schema, inject)

    def apply[H](label: String)(implicit ev: A <:< Union): OneOf[S, Union, A] =
      OneOf(label, schema, ev)
  }
}

/**
  * Represents a member of product type (case class)
  */
sealed abstract class StructureField[-S[_[_]], Struct, A] {
  def label: String
  def compile[F[_]](s: S[F]): Field[F, Struct, A]
}

final case class RequiredField[S[_[_]], Struct, A](
    label: String,
    schema: Schema[S, A],
    get: Struct => A
) extends StructureField[S, Struct, A] {
  override def toString(): String = s"$label: Optional($schema)"

  override def compile[F[_]](s: S[F]): Field[F, Struct, A] =
    Field.required(label, schema.compile(s), get)
}

final case class OptionalField[S[_[_]], Struct, A](
    label: String,
    schema: Schema[S, A],
    get: Struct => Option[A]
) extends StructureField[S, Struct, Option[A]] {
  override def toString(): String = s"$label: Required($schema)"

  def compile[F[_]](s: S[F]): Field[F, Struct, Option[A]] =
    Field.optional(label, schema.compile(s), get)
}

final case class OneOf[-S[_[_]], U, A](
    label: String,
    schema: Schema[S, A],
    inject: A => U
) {
  override def toString(): String = s"$label: $schema"
  def apply(a: A): OneOf.WithValue[S, U, A] =
    OneOf.WithValue(this, a)
  def compile[F[_]](s: S[F]): Alt[F, U, A] =
    new Alt(label, schema.compile(s), inject)
}

object OneOf {
  case class WithValue[-S[_[_]], U, A](alt: OneOf[S, U, A], value: A)
}
