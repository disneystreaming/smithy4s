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

package smithy4s.optics

// inspired by and adapted from https://www.optics.dev/Monocle/ under the MIT license

/**
 * Optional can be seen as the weak intersection between a [[Lens]]
 * and a [[Prism]]. It contains the same `replace` function as a [[Lens]]
 * and the same `getOption` function of a [[Prism]].
 */
trait Optional[S, A] { self =>

  /**
   * Returns a [[Some]] of A from S if it is able to obtain an A.
   * Else returns [[None]].
   */
  def getOption(s: S): Option[A]

  /** Provides a function to replace the target of the [[Lens]] */
  def replace(a: A): S => S

  /** Modify the target of the [[Optional]] with a function from A => A */
  def modify(f: A => A): S => S =
    s => getOption(s).fold(s)(a => replace(f(a))(s))

  /** Compose this [[Optional]] with another [[Optional]]. */
  final def andThen[A0](that: Optional[A, A0]): Optional[S, A0] =
    new Optional[S, A0] {
      def getOption(s: S): Option[A0] =
        self.getOption(s).flatMap(that.getOption)
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
      evA: A =:= A0
  ): Optional[S, A0] =
    evA.substituteCo[Optional[S, *]](this)
}
