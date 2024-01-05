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
  def project(s: S): Option[A]

  /** Returns an S given an A */
  def inject(a: A): S

  /** Modify the target of the [[Prism]] with a function from A => A */
  override final def modify(f: A => A): S => S =
    s => project(s).fold(s)(a => inject(f(a)))

  /** Provides a function to replace the target of the [[Prism]] */
  def replace(a: A): S => S =
    modify(_ => a)

  /** Compose this [[Prism]] with another [[Prism]]. */
  final def andThen[A0](that: Prism[A, A0]): Prism[S, A0] =
    new Prism[S, A0] {
      def project(s: S): Option[A0] =
        self.project(s).flatMap(that.project)
      def inject(a: A0): S =
        self.inject(that.inject(a))
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
      @annotation.unused evA: A =:= A0
  ): Prism[S, A0] =
    // safe due to A =:= A0
    this.asInstanceOf[Prism[S, A0]]

  /**
   * Helper function for targeting the value inside of a [[smithy4s.Newtype]]
   * or other type with an implicit [[Bijection]] available.
   */
  final override def value[A0](implicit
      bijection: Bijection[A0, A]
  ): Prism[S, A0] =
    new Prism[S, A0] {
      def project(s: S): Option[A0] = self.project(s).map(bijection.from)
      def inject(a: A0): S = self.inject(bijection.to(a))
    }
}

object Prism {

  /**
   * Construct a new [[Prism]] by providing functions for getting
   * an Option[A] from S and getting an S given an A.
   */
  def apply[S, A](_get: S => Option[A])(_inject: A => S): Prism[S, A] =
    new Prism[S, A] {
      def project(s: S): Option[A] = _get(s)
      def inject(a: A): S = _inject(a)
    }

  /**
   * Construct a new [[Prism]] with a [[PartialFunction]] to avoid needing
   * to exhaustively handle all possible `S` in the provided get function.
   */
  def partial[S, A](get: PartialFunction[S, A])(inject: A => S): Prism[S, A] =
    Prism[S, A](get.lift)(inject)
}
