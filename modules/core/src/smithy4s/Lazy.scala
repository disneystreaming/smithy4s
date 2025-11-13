/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s

sealed trait Lazy[A] {
  def value: A
  final def map[B](f: A => B): Lazy[B] = Lazy.Mapped(this, f)
}

object Lazy {
  def apply[A](a: => A): Lazy[A] = new Root(() => a)

  final class Root[A](make: () => A) extends Lazy[A] {
    protected var thunk: () => A = make
    lazy val value: A = {
      val result = thunk()
      thunk = null
      result
    }
  }

  final case class Mapped[A, B](left: Lazy[A], f: A => B) extends Lazy[B] {
    lazy val value = f(left.value)
  }
}
