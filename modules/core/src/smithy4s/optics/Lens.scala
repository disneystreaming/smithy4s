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

import smithy4s.Bijection

// inspired by and adapted from https://www.optics.dev/Monocle/ under the MIT license

/**
 * Lens implementation which can be used to abstract over accessing/updating
 * a member of a product type
 */
trait Lens[S, A] extends Optional[S, A] { self =>

  /** Retrieve the target of the [[Lens]] */
  def get(s: S): A

  /** Provides a function to replace the target of the [[Lens]] */
  def replace(a: A): S => S

  /** Retrieve the target of the [[Lens]] as an Optional (implemented to conform to [[Optional]]) */
  final def getOption(s: S): Option[A] = Some(get(s))

  /** Modify the target of the [[Lens]] with a function from A => A */
  override final def modify(f: A => A): S => S =
    s => replace(f(get(s)))(s)

  /**
   * Compose this [[Lens]] with another [[Lens]].
   * The result will be a lens that starts with the source
   * of the first lens and points to the target of the second
   * lens.
   */
  final def andThen[A0](that: Lens[A, A0]): Lens[S, A0] =
    new Lens[S, A0] {
      def get(s: S): A0 =
        that.get(self.get(s))
      def replace(a: A0): S => S =
        self.modify(that.replace(a))
    }

  /** 
   * Allows abstracting over an optional target by pointing to 
   * the inside of the optional value (the value inside of the [[Some]]).
   */
  final override def some[A0](implicit
      ev1: A =:= Option[A0]
  ): Optional[S, A0] =
    adapt[Option[A0]].andThen(
      Prism[Option[A0], A0](identity)(Some(_))
    )

  private[this] final def adapt[A0](implicit
      evA: A =:= A0
  ): Lens[S, A0] =
    evA.substituteCo[Lens[S, *]](this)

  /**
   * Helper function for targeting the value inside of a [[smithy4s.Newtype]]
   * or other type with an implicit [[Bijection]] available.
   */
  final def value[A0](implicit bijection: Bijection[A0, A]): Lens[S, A0] =
    new Lens[S, A0] {
      def get(s: S): A0 = bijection.from(self.get(s))
      def replace(a: A0): S => S = self.replace(bijection.from(a))
    }
}

object Lens {

  /**
   * Construct a new [[Lens]] by providing functions for getting
   * A from S and updating S given a new A.
   */
  def apply[S, A](_get: S => A)(_replace: A => S => S): Lens[S, A] =
    new Lens[S, A] {
      def get(s: S): A = _get(s)
      def replace(a: A): S => S = _replace(a)
    }

}
