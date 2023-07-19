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
trait Lens[S, A] extends Optional[S, A] { self =>
  def get(s: S): A
  def replace(a: A): S => S

  final def getOption(s: S): Option[A] = Some(get(s))

  override final def modify(f: A => A): S => S =
    s => replace(f(get(s)))(s)

  final def andThen[A0](that: Lens[A, A0]): Lens[S, A0] =
    new Lens[S, A0] {
      def get(s: S): A0 =
        that.get(self.get(s))
      def replace(a: A0): S => S =
        self.modify(that.replace(a))
    }

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

  final def value[A0](implicit bijection: Bijection[A0, A]): Lens[S, A0] =
    new Lens[S, A0] {
      def get(s: S): A0 = bijection.from(self.get(s))
      def replace(a: A0): S => S = self.replace(bijection.from(a))
    }
}

object Lens {

  def apply[S, A](_get: S => A)(_replace: A => S => S): Lens[S, A] =
    new Lens[S, A] {
      def get(s: S): A = _get(s)
      def replace(a: A): S => S = _replace(a)
    }

}
