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
import scala.collection.mutable.ArrayBuilder

trait PathEncode[A] { self =>
  def encode(ab: ArrayBuilder[String], a: A): Unit
  def encodeGreedy(ab: ArrayBuilder[String], a: A): Unit

  def contramap[B](from: B => A): PathEncode[B] = new PathEncode[B] {
    def encode(ab: ArrayBuilder[String], b: B): Unit =
      self.encode(ab, from(b))
    def encodeGreedy(ab: ArrayBuilder[String], b: B): Unit =
      self.encodeGreedy(ab, from(b))
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
        def encode(ab: ArrayBuilder[String], a: A): Unit = {
          ab += f(a)
        }

        def encodeGreedy(ab: ArrayBuilder[String], a: A): Unit = {
          ab ++= f(a).split('/')
        }
      }
    }

    def fromToString[A]: Make[A] = from(_.toString)

    def noop[A]: Make[A] = Hinted.static[MaybePathEncode, A](None)
  }

}
