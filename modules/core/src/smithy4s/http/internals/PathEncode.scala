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

package smithy4s.http.internals

import smithy4s.capability.Contravariant

trait PathEncode[A] { self =>
  def encode(a: A): List[String]
  def encodeGreedy(a: A): List[String]

  def contramap[B](from: B => A): PathEncode[B] = new PathEncode[B] {
    def encode(b: B): List[String] = self.encode(from(b))
    def encodeGreedy(b: B): List[String] = self.encodeGreedy(from(b))
  }
}

object PathEncode {

  type MaybePathEncode[A] = Option[PathEncode[A]]

  implicit val contravariantInstance: Contravariant[PathEncode] =
    new Contravariant[PathEncode] {
      def contramap[A, B](fa: PathEncode[A])(f: B => A): PathEncode[B] =
        fa.contramap(f)
    }
  def raw[A](f: A => String): PathEncode[A] = {
    new PathEncode[A] {
      def encode(a: A): List[String] = {
        List(f(a))
      }

      def encodeGreedy(a: A): List[String] = {
        f(a).split('/').toList
      }
    }
  }

  def from[A](f: A => String): MaybePathEncode[A] = {
    Some {
      raw(f)
    }
  }
  def fromToString[A]: MaybePathEncode[A] = from(_.toString)
}
