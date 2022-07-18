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

  @deprecated("use unsafe instead")
  def unchecked(a: A): B = unsafe(a)

  final val asFunction: A => Either[ConstraintError, B] =
    (a: A) =>
      apply(a).left.map(msg =>
        ConstraintError(Hints.Binding.StaticBinding(tag, constraint), msg)
      )

  final val asThrowingFunction: A => B =
    apply(_) match {
      case Left(msg) =>
        throw ConstraintError(Hints.Binding.StaticBinding(tag, constraint), msg)
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

  type Aux[C, A, B] = Refinement[A, B] { type Constraint = C }

}
