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

package smithy4s

sealed trait Validator[A, B] {
  def validate(value: A): Either[String, B]

  def toSchema(a: Schema[A]): Schema[B]

  def alsoValidating[C](constraint: C)(implicit
      ev: RefinementProvider.Simple[C, A]
  ): Validator[A, B]
}

object Validator {

  def of[A, B](bijection: Bijection[A, B]): ValidatorBuilder[A, B] =
    new ValidatorBuilder[A, B](bijection)

  final class ValidatorBuilder[A, B] private[smithy4s] (
      bijection: Bijection[A, B]
  ) {
    def validating[C](constraint: C)(implicit
        ev: RefinementProvider.Simple[C, A]
    ): Validator[A, B] =
      new ValidatorImpl[A, B](List(ev.make(constraint)), bijection)
  }

  private class ValidatorImpl[A, B](
      refinements: List[Refinement.Aux[_, A, A]],
      bijection: Bijection[A, B]
  ) extends Validator[A, B] {

    override def validate(value: A): Either[String, B] = {
      refinements
        .foldLeft(Right(value): Either[String, A]) {
          case (valueOrError, refinement) =>
            valueOrError.flatMap(refinement.apply)
        }
        .map(bijection.apply)
    }

    override def alsoValidating[C](constraint: C)(implicit
        ev: RefinementProvider.Simple[C, A]
    ): Validator[A, B] =
      new ValidatorImpl[A, B](refinements :+ ev.make(constraint), bijection)

    override def toSchema(a: Schema[A]): Schema[B] = {
      refinements
        .foldLeft(a) { (schema, refinement) =>
          schema.refined[A](refinement)
        }
        .biject(bijection)
    }
  }
}
