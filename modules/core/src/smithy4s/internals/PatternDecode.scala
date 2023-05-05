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

package smithy4s.internals

private[internals] trait PatternDecode[A] { self =>
  def decode(in: String): A

  def map[B](from: A => B): PatternDecode[B] = new PatternDecode[B] {
    def decode(in: String): B = from(self.decode(in))
  }
}

private[internals] object PatternDecode {

  type MaybePatternDecode[A] = Option[PatternDecode[A]]

  def raw[A](f: String => A): PatternDecode[A] = {
    new PatternDecode[A] {
      def decode(in: String): A = {
        f(in)
      }
    }
  }

  def from[A](f: String => A): MaybePatternDecode[A] = {
    Some {
      raw(f)
    }
  }
}
