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

package smithy4s

/**
  * A hint is an arbitrary piece of data that can be added to a schema,
  * at the struct level, or at the field/member level.
  *
  * You can think of it as an annotation that can communicate
  * additional information to encoders/decoders (for instance, a change
  * in a label, a regex pattern some string should abide by, a range, etc)
  *
  * This `Hints` interface is a container for hints.
  */
trait Hints {
  def isEmpty: Boolean
  def toMap: Map[Hints.Key[_], Hint]
  def all: Iterable[Hints.Binding[_]]
  def get[A](implicit key: Hints.Key[A]): Option[A]
  final def get[A](key: Hints.Key.Has[A]): Option[A] = get(key.getKey)
  final def get[T](nt: Newtype[T]): Option[nt.Type] = get(nt.key)
  def remove[A](implicit key: Hints.Key[A]): Hints
  def ++(other: Hints): Hints
}

object Hints {

  def apply[S](bindings: Hint*): Hints = {
    new Impl(bindings.map(_.tuple: (Key[_], Hint)).toMap)
  }

  trait Schematic[F[_]] {
    def withHints[A](fa: F[A], hints: Hints): F[A]
  }

  class Schema[S[x[_]] <: Schematic[x], A](
      a: schematic.Schema[S, A],
      hints: Hints
  ) extends schematic.Schema[S, A] {
    def compile[F[_]](s: S[F]): F[A] = s.withHints(a.compile(s), hints)
    def addHints(other: Hints) = new Schema[S, A](a, hints ++ other)
  }

  trait ClosedSyntax[S[x[_]] <: Schematic[x]] {
    def withHints[A](
        a: schematic.Schema[S, A],
        hints: Hints
    ): schematic.Schema[S, A] =
      new Schema(a, hints)
  }

  // NB: having Key be contravariant seems to break Scala 3

  /**
    * Key used to store and access hints.
    */

  trait Key[A] extends HasId

  object Key {
    trait Has[A] {
      def getKey: Key[A]
    }

    trait Companion[A] extends Key[A] with Has[A] {
      implicit val keyInstance: Key[A] = this
      final override def getKey: Key[A] = this
    }
  }

  private[smithy4s] class Impl(
      val toMap: Map[Key[_], Hint]
  ) extends Hints {
    val isEmpty = toMap.isEmpty
    def all: Iterable[Hint] = toMap.values
    def get[A](implicit key: Key[A]): Option[A] =
      toMap.get(key).map { case Binding(_, value) =>
        value.asInstanceOf[A]
      }
    def remove[A](implicit key: Hints.Key[A]): Hints =
      new Impl(toMap - key)
    def ++(other: Hints): Hints = {
      new Impl(toMap ++ other.toMap)
    }
    override def toString(): String =
      s"Hints(${all.map(_.value).mkString(", ")})"
  }

  case class Binding[A](key: Key[A], value: A) {
    def tuple: (Key[A], Binding[A]) = key -> this
  }

  object Binding {
    implicit def fromValue[A, AA <: A](value: AA)(implicit
        key: Key[A]
    ): Binding[A] =
      Binding(key, value)
  }

}
