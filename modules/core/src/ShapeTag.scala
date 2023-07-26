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
  * A tag that can be used as keys for higher-kinded maps
  *
  * NB : cannot source the id from the schema because of complex initialisation problems
  * occurring notably because of recursive references in the generated code. For instance, the `Documentation`
  * new-type has a Documentation hint.
  */
trait ShapeTag[A] {
  def id: ShapeId
  def schema: Schema[A]
}

object ShapeTag {
  def apply[A](implicit tag: ShapeTag[A]): ShapeTag[A] = tag

  trait Has[A] {
    def getTag: ShapeTag[A]
  }

  trait Companion[A] extends ShapeTag[A] with Has[A] {
    implicit val tagInstance: ShapeTag[A] = this
    final override def getTag: ShapeTag[A] = this

    object hint {
      def unapply(h: Hints): Option[A] = h.get[A]
    }
  }

  trait $Companion[A] extends Has[A] {
    def $id: ShapeId
    def $schema: Schema[A]
    implicit val tagInstance: ShapeTag[A] = new ShapeTag[A] {
      def id: ShapeId = $id
      def schema: Schema[A] = $schema
    }
    final override def getTag: ShapeTag[A] = tagInstance

    object hint {
      def unapply(h: Hints): Option[A] = h.get[A]
    }
  }

  // Trying to provide `.schema` accessors for generated types, which hold
  // `val $schema` values since 0.18 to prevent collision.
  implicit def $companionToShapeTag[A](companion: $Companion[A]): ShapeTag[A] =
    companion.getTag
  implicit def newTypeToShapeTag[A, T](a: Newtype.Aux[A, T]): ShapeTag[T] =
    a.tag
}
