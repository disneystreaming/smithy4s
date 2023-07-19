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
 * a member of a coproduct type
 */
trait Prism[S, A] extends Optional[S, A] { self =>

  /**
   * Returns a [[Some]] of A from S if it is able to obtain an A.
   * Else returns [[None]].
   */
  def getOption(s: S): Option[A]

  /** Returns an S given an A */
  def project(a: A): S

  /** Modify the target of the [[Prism]] with a function from A => A */
  override final def modify(f: A => A): S => S =
    s => getOption(s).fold(s)(a => project(f(a)))

  /** Provides a function to replace the target of the [[Prism]] */
  def replace(a: A): S => S =
    modify(_ => a)

  /** Compose this [[Prism]] with another [[Prism]]. */
  final def andThen[A0](that: Prism[A, A0]): Prism[S, A0] =
    new Prism[S, A0] {
      def getOption(s: S): Option[A0] =
        self.getOption(s).flatMap(that.getOption)
      def project(a: A0): S =
        self.project(that.project(a))
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
  ): Prism[S, A0] =
    evA.substituteCo[Prism[S, *]](this)

  /**
   * Helper function for targeting the value inside of a [[smithy4s.Newtype]]
   * or other type with an implicit [[Bijection]] available.
   */
  final def value[A0](implicit bijection: Bijection[A0, A]): Prism[S, A0] =
    new Prism[S, A0] {
      def getOption(s: S): Option[A0] = self.getOption(s).map(bijection.from)
      def project(a: A0): S = self.project(bijection.to(a))
    }
}

object Prism {

  /**
   * Construct a new [[Prism]] by providing functions for getting
   * an Option[A] from S and getting an S given an A.
   */
  def apply[S, A](_get: S => Option[A])(_project: A => S): Prism[S, A] =
    new Prism[S, A] {
      def getOption(s: S): Option[A] = _get(s)
      def project(a: A): S = _project(a)
    }

  /**
   * Construct a new [[Prism]] with a [[PartialFunction]] to avoid needing
   * to exhaustively handle all possible `S` in the provided get function.
   */
  def partial[S, A](get: PartialFunction[S, A])(project: A => S): Prism[S, A] =
    Prism[S, A](get.lift)(project)
}
