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
  * A map of schemas indexed by tag.
  */
sealed trait SchemaIndex {

  def isEmpty: Boolean
  def tags: Iterable[ShapeTag[_]]
  def get[A](implicit key: ShapeTag[A]): Option[Schema[A]]
  final def get[A](key: ShapeTag.Has[A]): Option[Schema[A]] = get(key.getTag)
  final def get[T](nt: Newtype[T]): Option[Schema[nt.Type]] = get(nt.tag)
  def get(shapeId: ShapeId): Option[Schema[_]]
  def ++(other: SchemaIndex): SchemaIndex

}

object SchemaIndex {

  def apply[S](bindings: Binding[_]*): SchemaIndex = {
    new Impl(bindings.map(b => b.tuple: (ShapeTag[_], Schema[_])).toMap)
  }

  trait Schematic[F[_]] {
    def withHints[A](fa: F[A], hints: Hints): F[A]
  }

  private[smithy4s] class Impl(
      val toMap: Map[ShapeTag[_], Schema[_]]
  ) extends SchemaIndex {
    val isEmpty = toMap.isEmpty
    def tags: Iterable[ShapeTag[_]] = toMap.keys
    def get[A](implicit tag: ShapeTag[A]): Option[Schema[A]] =
      toMap.get(tag).map(_.asInstanceOf[Schema[A]])
    def get(shapeId: ShapeId): Option[Schema[_]] =
      toMap.find(_._1.id == shapeId).map(_._2)
    def ++(other: SchemaIndex): SchemaIndex = other match {
      case o: Impl => new Impl(toMap ++ o.toMap)
    }
    override def toString(): String =
      s"SchemaIndex(${tags.map(_.id).mkString(",")})"
  }

  case class Binding[A](tag: ShapeTag[A], schema: Schema[A]) {
    def tuple: (ShapeTag[_], Schema[_]) = (tag, schema)
  }

  case object Binding {

    implicit def fromKey[A](tag: ShapeTag[A])(implicit
        schema: Schema[A]
    ): Binding[A] =
      Binding[A](tag, schema)

    implicit def fromNewType[A](nt: Newtype[A])(implicit
        schema: Schema[nt.Type]
    ): Binding[nt.Type] =
      Binding(nt.tag, schema)
  }

}
