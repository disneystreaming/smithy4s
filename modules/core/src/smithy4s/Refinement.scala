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

  final val asFunction: A => Either[ConstraintError, B] =
    (a: A) =>
      apply(a).left.map(msg =>
        ConstraintError(Hints.Binding(tag, constraint), msg)
      )

  final val asThrowingFunction: A => B =
    apply(_) match {
      case Left(msg) =>
        throw ConstraintError(Hints.Binding(tag, constraint), msg)
      case Right(b) => b
    }

  final def contramap[A0](f: A0 => A): Refinement.Aux[Constraint, A0, B] =
    new Refinement[A0, B] {
      type Constraint = self.Constraint
      def tag: ShapeTag[Constraint] = self.tag
      def constraint: Constraint = self.constraint
      def apply(a0: A0): Either[String, B] = self(f(a0))
    }

  final def map[B0](f: B => B0): Refinement.Aux[Constraint, A, B0] =
    new Refinement[A, B0] {
      type Constraint = self.Constraint
      def tag: ShapeTag[Constraint] = self.tag
      def constraint: Constraint = self.constraint
      def apply(a: A): Either[String, B0] = self(a).map(f)
    }

}

object Refinement {

  type Aux[C, A, B] = Refinement[A, B] { type Constraint = C }

}
