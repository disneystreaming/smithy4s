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
trait Prism[S, A] extends Optional[S, A] { self =>
  def getOption(s: S): Option[A]
  def project(a: A): S

  override final def modify(f: A => A): S => S =
    s => getOption(s).fold(s)(a => project(f(a)))

  def replace(a: A): S => S =
    modify(_ => a)

  final def andThen[A0](that: Prism[A, A0]): Prism[S, A0] =
    new Prism[S, A0] {
      def getOption(s: S): Option[A0] =
        self.getOption(s).flatMap(that.getOption)
      def project(a: A0): S =
        self.project(that.project(a))
    }

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

  final def value[A0](implicit bijection: Bijection[A0, A]): Prism[S, A0] =
    new Prism[S, A0] {
      def getOption(s: S): Option[A0] = self.getOption(s).map(bijection.from)
      def project(a: A0): S = self.project(bijection.from(a))
    }
}

object Prism {
  def apply[S, A](_get: S => Option[A])(_project: A => S): Prism[S, A] =
    new Prism[S, A] {
      def getOption(s: S): Option[A] = _get(s)
      def project(a: A): S = _project(a)
    }

  def partial[S, A](get: PartialFunction[S, A])(project: A => S): Prism[S, A] =
    Prism[S, A](get.lift)(project)
}
