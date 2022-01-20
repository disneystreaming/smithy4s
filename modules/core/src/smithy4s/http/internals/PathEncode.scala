/*
 *  Copyright 2021 Disney Streaming
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

import smithy4s.internals.Hinted

trait PathEncode[A] { self =>
  def encode(sb: StringBuilder, a: A): Unit
  def encodeGreedy(sb: StringBuilder, a: A): Unit

  def contramap[B](from: B => A): PathEncode[B] = new PathEncode[B] {
    def encode(sb: StringBuilder, b: B): Unit = self.encode(sb, from(b))

    def encodeGreedy(sb: StringBuilder, b: B): Unit =
      self.encodeGreedy(sb, from(b))
  }
}

object PathEncode {

  type MaybePathEncode[A] = Option[PathEncode[A]]
  type Make[A] = Hinted[MaybePathEncode, A]

  object Make {
    def from[A](f: A => String): Make[A] = Hinted.static[MaybePathEncode, A] {
      Some {
        raw(f)
      }
    }

    def raw[A](f: A => String): PathEncode[A] = {
      new PathEncode[A] {
        def encode(sb: StringBuilder, a: A): Unit = {
          val _ = sb.append(URIEncoderDecoder.encode(f(a)))
        }
        def encodeGreedy(sb: StringBuilder, a: A): Unit = {
          f(a).split('/').foreach {
            case s if s.isEmpty() => ()
            case s => sb.append('/').append(URIEncoderDecoder.encode(s))
          }
        }
      }
    }

    def fromToString[A]: Make[A] = from(_.toString)

    def noop[A]: Make[A] = Hinted.static[MaybePathEncode, A](None)

  }

}
