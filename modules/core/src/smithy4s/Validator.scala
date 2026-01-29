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

package smithy4s

sealed trait Validator[A, B] { self =>
  def validate(value: A): Either[String, B]

  // todo: deprecate
  def toSchema(a: Schema[A]): Schema[B]

  def alsoValidating[C](constraint: C)(implicit
      ev: RefinementProvider.Simple[C, A]
  ): Validator[A, B] = toBuilder.validating(constraint).build()

  def toBuilder: Validator.Builder[A, B]

}

object Validator {

  trait Builder[A, B] { self =>
    def validating[C](constraint: C)(implicit
        ev: RefinementProvider.Simple[C, A]
    ): Builder[A, B]

    def biject[B0](implicit bijection: Bijection[B, B0]): Builder[A, B0] =
      new Builder.Bijected(self, bijection)

    def build(): Validator[A, B]
  }

  private[smithy4s] final class RefinedSyntax[A](builder: Builder[A, A]) {
    def refined[C]: Builder.PartiallyAppliedRefinedBuilder[A, C] =
      new Builder.PartiallyAppliedRefinedBuilder[A, C](builder)
  }

  object Builder {

    implicit def toRefinedSyntax[A, B](builder: Builder[A, B])(implicit
        ev: A =:= B
    ): RefinedSyntax[A] =
      new RefinedSyntax(builder.asInstanceOf[Builder[A, A]])

    private[smithy4s] class PartiallyAppliedRefinedBuilder[A, B](
        base: Builder[A, A]
    ) {
      def apply[C](c: C)(implicit
          ev: RefinementProvider[C, A, B]
      ): Builder[B, B] = new Builder.Refined(base, ev.make(c), Vector.empty)
    }

    trait Collection[Col[_], E] extends Builder[Col[E], Col[E]] {
      override def validating[C](constraint: C)(implicit
          ev: RefinementProvider.Simple[C, Col[E]]
      ): Builder.Collection[Col, E]

      def validatingMember[C](constraint: C)(implicit
          ev: RefinementProvider.Simple[C, E]
      ): Collection[Col, E]

    }

    trait KeyValue[K, V] extends Builder[Map[K, V], Map[K, V]] {
      override def validating[C](constraint: C)(implicit
          ev: RefinementProvider.Simple[C, Map[K, V]]
      ): KeyValue[K, V]

      def validatingKey[C](constraint: C)(implicit
          ev: RefinementProvider.Simple[C, K]
      ): KeyValue[K, V]

      def validatingValue[C](constraint: C)(implicit
          ev: RefinementProvider.Simple[C, V]
      ): KeyValue[K, V]

    }

    def simple[A]: Builder[A, A] = new Simple(Vector.empty)

    def list[E]: Builder.Collection[List, E] =
      collection(schema.CollectionTag.ListTag)

    def set[E]: Builder.Collection[Set, E] =
      collection(schema.CollectionTag.SetTag)

    def vector[E]: Builder.Collection[Vector, E] =
      collection(schema.CollectionTag.VectorTag)

    def indexedSeq[E]: Builder.Collection[IndexedSeq, E] =
      collection(schema.CollectionTag.IndexedSeqTag)

    def map[K, V]: Builder.KeyValue[K, V] =
      new KeyValueImpl[K, V](None, Vector.empty, Vector.empty)

    private def collection[C[_], E](
        tag: schema.CollectionTag[C]
    ): Builder.Collection[C, E] = collection(tag.iterator(_))

    private def collection[Col[_], E](
        getIterator: Col[E] => Iterator[E]
    ): Builder.Collection[Col, E] =
      new CollectionImpl(getIterator, None, Vector.empty)

    private final class Simple[A](
        refinements: Vector[Refinement[A, A]]
    ) extends Builder[A, A] { self =>

      override def validating[C](constraint: C)(implicit
          ev: RefinementProvider.Simple[C, A]
      ): Builder[A, A] =
        new Simple[A](refinements :+ ev.make(constraint))

      override def build(): Validator[A, A] = new Validator[A, A] {
        override def validate(value: A): Either[String, A] =
          refinements
            .foldLeft(Right(value): Either[String, A]) {
              case (valueOrError, refinement) =>
                valueOrError.flatMap(refinement.apply)
            }

        override def toSchema(a: Schema[A]): Schema[A] =
          refinements
            .foldLeft(a) { (schema, refinement) =>
              schema.refined[A](refinement)
            }

        override def toBuilder: Builder[A, A] = self

      }

    }

    private final class Bijected[A, B, B0](
        source: Builder[A, B],
        bijectTarget: Bijection[B, B0]
    ) extends Builder[A, B0] { self =>

      override def biject[B1](implicit
          bijection: Bijection[B0, B1]
      ): Builder[A, B1] =
        new Bijected(
          source,
          bijectTarget.imapTarget(bijection)
        )

      override def validating[C](constraint: C)(implicit
          ev: RefinementProvider.Simple[C, A]
      ): Builder[A, B0] =
        new Bijected(
          source.validating(constraint),
          bijectTarget
        )

      override def build(): Validator[A, B0] = new Validator[A, B0] {
        val sourceValidator = source.build()

        override def validate(value: A): Either[String, B0] =
          sourceValidator.validate(value).map(bijectTarget.to)

        override def toSchema(a: Schema[A]): Schema[B0] =
          sourceValidator.toSchema(a).biject(bijectTarget)

        override def toBuilder: Builder[A, B0] = self
      }
    }

    private final class Refined[A, B](
        mainBuilder: Builder[A, A],
        refinement: Refinement[A, B],
        additionalRefinements: Vector[Refinement[B, B]]
    ) extends Builder[B, B] { self =>

      private val mainValidator = mainBuilder.build()
      override def validating[C](
          c: C
      )(implicit ev: RefinementProvider.Simple[C, B]): Builder[B, B] =
        new Refined(
          mainBuilder,
          refinement,
          additionalRefinements = additionalRefinements :+ ev.make(c)
        )

      override def build(): Validator[B, B] = new Validator[B, B] {
        override def validate(value: B): Either[String, B] =
          mainValidator
            .validate(refinement.from(value))
            .flatMap(_ =>
              additionalRefinements.foldLeft(right(value)) { case (acc, ref) =>
                acc.flatMap(_ => ref.apply(value))
              }
            )
        override def toSchema(a: Schema[B]): Schema[B] = a

        override def toBuilder: Builder[B, B] = self

      }

    }

    private final class KeyValueImpl[K, V](
        mainBuilder: Option[Builder[Map[K, V], Map[K, V]]],
        keyRefinements: Vector[Refinement[K, K]],
        valueRefinements: Vector[Refinement[V, V]]
    ) extends Builder.KeyValue[K, V] { self =>

      override def validating[C](constraint: C)(implicit
          ev: RefinementProvider.Simple[C, Map[K, V]]
      ): KeyValue[K, V] =
        new KeyValueImpl(
          mainBuilder = mainBuilder
            .map(_.validating(constraint))
            .orElse(Some(simple.validating(constraint))),
          keyRefinements = keyRefinements,
          valueRefinements = valueRefinements
        )

      override def validatingKey[C](constraint: C)(implicit
          ev: RefinementProvider.Simple[C, K]
      ): KeyValue[K, V] = new KeyValueImpl(
        mainBuilder = mainBuilder,
        keyRefinements = keyRefinements :+ ev.make(constraint),
        valueRefinements = valueRefinements
      )

      override def validatingValue[C](constraint: C)(implicit
          ev: RefinementProvider.Simple[C, V]
      ): KeyValue[K, V] = new KeyValueImpl(
        mainBuilder = mainBuilder,
        keyRefinements = keyRefinements,
        valueRefinements = valueRefinements :+ ev.make(constraint)
      )

      override def build(): Validator[Map[K, V], Map[K, V]] =
        new Validator[Map[K, V], Map[K, V]] {
          val mainValidator = mainBuilder.map(_.build())
          override def validate(
              value: Map[K, V]
          ): Either[String, Map[K, V]] = {
            val mainValidated =
              mainValidator.map(_.validate(value)).getOrElse(right(()))
            def validateKeySet = validateCollection(
              value.iterator
            ) { case (k, v) =>
              validateRefinements(keyRefinements)(k).flatMap(_ =>
                validateRefinements(valueRefinements)(v)
              )
            }
            mainValidated.flatMap(_ => validateKeySet).map(_ => value)
          }

          override def toSchema(a: Schema[Map[K, V]]): Schema[Map[K, V]] = {
            val main = mainValidator.map(_.toSchema(a)).getOrElse(a)
            main match {
              case mapSchema @ Schema.MapSchema(_, _, key, value) =>
                val newKey = keyRefinements.foldLeft(key) {
                  case (acc, refinement) => acc.refined(refinement)
                }
                val newValue = valueRefinements.foldLeft(value) {
                  case (acc, refinement) => acc.refined(refinement)
                }
                mapSchema.copy(key = newKey, value = newValue)
              case _ => main
            }
          }

          override def toBuilder: Builder[Map[K, V], Map[K, V]] = self

        }

    }

    private final class CollectionImpl[Col[_], Elem](
        getIterator: Col[Elem] => Iterator[Elem],
        mainBuilder: Option[Builder[Col[Elem], Col[Elem]]],
        memberRefinements: Vector[Refinement[Elem, Elem]]
    ) extends Builder.Collection[Col, Elem] { self =>

      override def validating[C](constraint: C)(implicit
          ev: RefinementProvider.Simple[C, Col[Elem]]
      ): Collection[Col, Elem] =
        new CollectionImpl(
          getIterator,
          mainBuilder = mainBuilder
            .map(_.validating(constraint))
            .orElse(Some(simple.validating(constraint))),
          memberRefinements = memberRefinements
        )

      override def validatingMember[C](
          constraint: C
      )(implicit
          ev: RefinementProvider.Simple[C, Elem]
      ): Collection[Col, Elem] =
        new CollectionImpl(
          getIterator,
          mainBuilder,
          memberRefinements :+ ev.make(constraint)
        )

      override def build(): Validator[Col[Elem], Col[Elem]] =
        new Validator[Col[Elem], Col[Elem]] {
          val mainValidator = mainBuilder.map(_.build())
          override def validate(
              value: Col[Elem]
          ): Either[String, Col[Elem]] = {
            val elementsValidated = validateCollection(getIterator(value)) {
              validateRefinements(memberRefinements)
            }
            val mainValidated =
              mainValidator
                .map(_.validate(value).map(_ => ()))
                .getOrElse(
                  right(())
                )
            mainValidated.flatMap(_ => elementsValidated.map(_ => value))
          }

          override def toSchema(a: Schema[Col[Elem]]): Schema[Col[Elem]] = {
            val main = mainValidator.map(_.toSchema(a)).getOrElse(a)
            main match {
              case collectionSchema @ Schema.CollectionSchema(
                    shapeId,
                    hints,
                    tag,
                    member
                  ) =>
                val newMember = memberRefinements.foldLeft(member) {
                  case (acc, refinement) => acc.refined(refinement)
                }
                collectionSchema.copy(member = newMember)
              case _ => main
            }
          }

          override def toBuilder: Builder[Col[Elem], Col[Elem]] = self
        }
    }

    private def right[A](value: A): Either[String, A] = Right(value)

    private def validateRefinements[A](
        refinements: Vector[Refinement[A, A]]
    )(elem: A): Either[String, Unit] = refinements
      .foldLeft(right(elem)) { case (result, constraint) =>
        result.flatMap(_ => constraint(elem))
      }
      .map(_ => ())

    private def validateCollection[A](
        it: Iterator[A]
    )(validate: A => Either[String, Unit]): Either[String, Unit] = {
      var acc: Either[String, Unit] = Right(())
      while (it.hasNext && acc.isRight) {
        acc = validate(it.next())
      }
      acc
    }

  }

  @deprecated
  def of[A, B](bijection: Bijection[A, B]): ValidatorBuilder[A, B] =
    new ValidatorBuilder[A, B](bijection)

  @deprecated
  final class ValidatorBuilder[A, B] private[smithy4s] (
      bijection: Bijection[A, B]
  ) {
    def validating[C](constraint: C)(implicit
        ev: RefinementProvider.Simple[C, A]
    ): Validator[A, B] =
      Validator.Builder
        .simple[A]
        .validating(constraint)
        .biject(bijection)
        .build()
  }

}
