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
  def all: Iterable[Hints.Binding]
  def toMap: Map[ShapeId, Hints.Binding]
  def get[A](implicit key: ShapeTag[A]): Option[A]
  final def has[A](implicit key: ShapeTag[A]): Boolean = this.get[A].isDefined
  final def get[A](key: ShapeTag.Has[A]): Option[A] = get(key.getTag)
  final def get[T](nt: Newtype[T]): Option[nt.Type] = get(nt.tag)
  final def filter(predicate: Hint => Boolean): Hints =
    Hints.fromSeq(all.filter(predicate).toSeq)
  final def filterNot(predicate: Hint => Boolean): Hints =
    filter(hint => !predicate(hint))
  def ++(other: Hints): Hints
}

object Hints {

  val empty: Hints = new Impl(Map.empty)

  def apply[S](bindings: Hint*): Hints = fromSeq(bindings)

  def fromSeq[S](bindings: Seq[Hint]): Hints = {
    new Impl(bindings.map {
      case b @ Binding.StaticBinding(k, _)  => k.id -> b
      case b @ Binding.DynamicBinding(k, _) => k -> b
    }.toMap)
  }

  private[smithy4s] final class Impl(
      val toMap: Map[ShapeId, Hint]
  ) extends Hints {
    val isEmpty = toMap.isEmpty
    def all: Iterable[Hint] = toMap.values
    def get[A](implicit key: ShapeTag[A]): Option[A] =
      toMap.get(key.id).flatMap {
        case Binding.StaticBinding(k, value) =>
          if (key.eq(k)) Some(value.asInstanceOf[A]) else None
        case Binding.DynamicBinding(_, value) =>
          Document.Decoder.fromSchema(key.schema).decode(value).toOption
      }
    def ++(other: Hints): Hints = {
      new Impl(toMap ++ other.toMap)
    }
    override def toString(): String =
      s"Hints(${all.mkString(", ")})"

    override def hashCode(): Int = toMap.hashCode()
    override def equals(obj: Any): Boolean = {
      obj.isInstanceOf[Impl] && obj.asInstanceOf[Impl].toMap == this.toMap
    }
  }

  sealed trait Binding extends Product with Serializable {
    def keyId: ShapeId
  }

  object Binding {
    final case class StaticBinding[A](key: ShapeTag[A], value: A)
        extends Binding {
      override def keyId: ShapeId = key.id
      override def toString: String = value.toString()
    }
    final case class DynamicBinding(keyId: ShapeId, value: Document)
        extends Binding {
      override def toString = Document.obj(keyId.show -> value).toString()
    }

    implicit def fromValue[A, AA <: A](value: AA)(implicit
        key: ShapeTag[A]
    ): Binding = StaticBinding(key, value)

    implicit def fromTuple(tup: (ShapeId, Document)): Binding =
      DynamicBinding(tup._1, tup._2)
  }

}
