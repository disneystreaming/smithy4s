/*
 *  Copyright 2021-2023 Disney Streaming
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

final class Lazy[A](make: () => A) {
  private[this] var thunk: () => A = make
  lazy val value: A = {
    val result = thunk()
    thunk = null
    result
  }

  def map[B](f: A => B): Lazy[B] = new Lazy(() => f(make()))

  override def equals(obj: Any): Boolean = obj match {
    case that: Lazy[_] => this.value == that.value
    case _             => false
  }

  override def hashCode(): Int = value.hashCode()

}

object Lazy {
  def apply[A](a: => A): Lazy[A] = new Lazy(() => a)
}
