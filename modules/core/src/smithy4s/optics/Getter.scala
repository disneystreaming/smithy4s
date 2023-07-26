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
trait Getter[S, A] { self =>

  /** Retrieve the target of the [[Lens]] */
  def get(s: S): A

  /**
   * Compose this [[Lens]] with another [[Lens]].
   * The result will be a lens that starts with the source
   * of the first lens and points to the target of the second
   * lens.
   */
  def andThen[A0](that: Getter[A, A0]): Getter[S, A0] =
    new Getter[S, A0] {
      def get(s: S): A0 = that.get(self.get(s))
    }

  /**
   * Helper function for targeting the value inside of a [[smithy4s.Newtype]]
   * or other type with an implicit [[Bijection]] available.
   */
  def value[A0](implicit
      bijection: Bijection[A, A0]
  ): Getter[S, A0] =
    new Getter[S, A0] {
      def get(s: S): A0 = bijection.to(self.get(s))
    }
}

object Getter {

  /**
   * Construct a new [[Lens]] by providing functions for getting
   * A from S and updating S given a new A.
   */
  def apply[S, A](_get: S => A): Getter[S, A] =
    new Getter[S, A] {
      def get(s: S): A = _get(s)
    }

}
