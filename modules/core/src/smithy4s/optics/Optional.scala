/*
 *  Copyright 2021-2023 Disney Streaming
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

package smithy4s.optics

import smithy4s.Bijection

// inspired by and adapted from https://www.optics.dev/Monocle/ under the MIT license

/**
 * Optional can be seen as the weak intersection between a [[Lens]]
 * and a [[Prism]]. It contains the same `replace` function as a [[Lens]]
 * and the same `project` function of a [[Prism]].
 */
trait Optional[S, A] { self =>

  /**
   * Returns a [[Some]] of A from S if it is able to obtain an A.
   * Else returns [[None]].
   */
  def project(s: S): Option[A]

  /** Provides a function to replace the target of the [[Lens]] */
  def replace(a: A): S => S

  /** Modify the target of the [[Optional]] with a function from A => A */
  def modify(f: A => A): S => S =
    s => project(s).fold(s)(a => replace(f(a))(s))

  /** Compose this [[Optional]] with another [[Optional]]. */
  final def andThen[A0](that: Optional[A, A0]): Optional[S, A0] =
    new Optional[S, A0] {
      def project(s: S): Option[A0] =
        self.project(s).flatMap(that.project)
      def replace(a: A0): S => S =
        self.modify(that.replace(a))
    }

  /** 
   * Allows abstracting over an optional target by pointing to 
   * the inside of the optional value (the value inside of the [[Some]]).
   */
  def some[A0](implicit
      ev1: A =:= Option[A0]
  ): Optional[S, A0] =
    adapt[Option[A0]].andThen(
      Prism[Option[A0], A0](identity)(Some(_))
    )

  private[this] final def adapt[A0](implicit
      @annotation.unused evA: A =:= A0
  ): Optional[S, A0] =
    // safe due to A =:= A0
    this.asInstanceOf[Optional[S, A0]]

  /**
   * Helper function for targeting the value inside of a [[smithy4s.Newtype]]
   * or other type with an implicit [[Bijection]] available.
   */
  def value[A0](implicit bijection: Bijection[A0, A]): Optional[S, A0] =
    new Optional[S, A0] {
      def project(s: S): Option[A0] = self.project(s).map(bijection.from(_))
      def replace(a: A0): S => S = self.replace(bijection.to(a))
    }
}
