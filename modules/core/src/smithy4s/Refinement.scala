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

package smithy4s

/**
  * A type-refinement, associated to a runtime-representation of a constraint.
  *
  * Represents the fact that you can go from A to B provided the value of tye A
  * abides by a given Constraint.
  */
trait Refinement[A, B] { self =>

  /**
    * The reified constraint associated to the refinement
    */
  type Constraint
  def tag: ShapeTag[Constraint]
  def constraint: Constraint
  def apply(a: A): Either[String, B]
  def from(b: B): A

  /**
    * Short circuits validation. This should only be used as last-resort
    * when it is impossible to implement schema compilers otherwise, such as ones
    * that create data out of thin air (random generators/default values/etc).
    */
  def unsafe(a: A): B

  final val asFunction: A => Either[ConstraintError, B] =
    (a: A) =>
      apply(a).left.map(msg =>
        ConstraintError(
          new Hints.Binding.StaticBinding[Constraint](tag, Lazy(constraint)),
          msg
        )
      )

  final val asThrowingFunction: A => B =
    apply(_) match {
      case Left(msg) =>
        throw ConstraintError(
          new Hints.Binding.StaticBinding[Constraint](tag, Lazy(constraint)),
          msg
        )
      case Right(b) => b
    }

  final def imapFull[A0, B0](
      bijectSource: Bijection[A, A0],
      bijectTarget: Bijection[B, B0]
  ): Refinement.Aux[Constraint, A0, B0] =
    new Refinement[A0, B0] {
      type Constraint = self.Constraint
      def tag: ShapeTag[Constraint] = self.tag
      def constraint: Constraint = self.constraint
      def apply(a0: A0): Either[String, B0] =
        self(bijectSource.from(a0)).map(bijectTarget)
      def unsafe(a0: A0): B0 = bijectTarget(self.unsafe(bijectSource.from(a0)))
      def from(b: B0): A0 = bijectSource(self.from(bijectTarget.from(b)))
    }

}

object Refinement {

  def drivenBy[C]: PartiallyApplyRefinementProvider[C] =
    new PartiallyApplyRefinementProvider[C]

  class PartiallyApplyRefinementProvider[C] {

    def apply[A, B](
        surjection: Surjection[A, B]
    )(implicit C: ShapeTag[C]): RefinementProvider[C, A, B] =
      new RefinementProvider[C, A, B] {
        val tag: ShapeTag[C] = ShapeTag[C]
        def make(c: C): Aux[C, A, B] = new Refinement[A, B] {
          type Constraint = C
          val tag: ShapeTag[C] = ShapeTag[C]
          def constraint: C = c
          def apply(a: A): Either[String, B] = surjection.to(a)
          def from(b: B): A = surjection.from(b)
          def unsafe(a: A): B = asThrowingFunction(a)
        }
      }

    def apply[A, B](
        to: A => Either[String, B],
        from: B => A
    )(implicit C: ShapeTag[C]): RefinementProvider[C, A, B] = apply(
      Surjection(to, from)
    )

    def contextual[A, B](build: C => Surjection[A, B])(implicit
        tagEvidence: ShapeTag[C]
    ): RefinementProvider[C, A, B] = new RefinementProvider[C, A, B] {
      val tag: ShapeTag[C] = tagEvidence
      def make(c: C): Aux[C, A, B] = {
        val surjection = build(c)
        new Refinement[A, B] {
          type Constraint = C
          val tag: ShapeTag[C] = tagEvidence
          def constraint: C = c
          def apply(a: A): Either[String, B] = surjection.to(a)
          def from(b: B): A = surjection.from(b)
          def unsafe(a: A): B = asThrowingFunction(a)
        }
      }
    }
  }

  type Aux[C, A, B] = Refinement[A, B] { type Constraint = C }

}
