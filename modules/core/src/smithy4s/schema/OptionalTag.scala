/*
 *  Copyright 2021-2025 Disney Streaming
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
package schema

trait OptionalTag[C[_]] { self =>
  def name: String

  def fromNullable[A](a: A): C[A] =
    if (a == null) none else some(a)
  def some[A](a: A): C[A]
  def none[A]: C[A]
  def map[A, B](a: C[A], fn: A => B): C[B] =
    fold[A, C[B]](a, (a: A) => some(fn(a)), none)

  def fromScalaOption[A](c: Option[A]): C[A] =
    c match {
      case Some(a) => some(a)
      case None    => none
    }
  def fold[A, B](c: C[A], isSome: A => B, isNone: => B): B

  def isNone[A](c: C[A]): Boolean
  def exists[A](c: C[A], fn: A => Boolean): Boolean
  def toScalaOption[A](c: C[A]): Option[A] =
    self.fold(c, Some[A](_), None)
}

object OptionalTag {

  case object ScalaOptionTag extends OptionalTag[Option] {
    override def name: String = "Option"
    override def some[A](a: A): Option[A] = Some(a)
    override def none[A]: Option[A] = None
    override def isNone[A](c: Option[A]): Boolean = c.isEmpty
    override def exists[A](c: Option[A], fn: A => Boolean): Boolean =
      c.exists(fn)
    override def fold[A, B](c: Option[A], isSome: A => B, isNone: => B): B =
      c.fold(isNone)(isSome)
    override def fromScalaOption[A](c: Option[A]): Option[A] = c
    override def toScalaOption[A](c: Option[A]): Option[A] = c
  }

}
