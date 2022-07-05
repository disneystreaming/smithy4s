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
package schema

sealed trait CollectionTag[C[_]] {
  def name: String

  def iterator[A](c: C[A]): Iterator[A]
  def build[A](put: (A => Unit) => Unit): C[A]

  def fromIterator[A](it: Iterator[A]): C[A] = build(put => it.foreach(put(_)))
  def empty[A]: C[A] = build(_ => ())
}

object CollectionTag {
  import scala.collection.{immutable => cols}

  case object List extends CollectionTag[List] {
    override def name: String = "List"

    override def iterator[A](c: List[A]): Iterator[A] = c.iterator

    override def build[A](put: (A => Unit) => Unit): List[A] = {
      val builder = cols.List.newBuilder[A]
      put(builder.+=(_))
      builder.result()
    }

  }

  case object Set extends CollectionTag[Set] {
    override def name: String = "Set"
    override def iterator[A](c: Set[A]): Iterator[A] = c.iterator

    override def build[A](put: (A => Unit) => Unit): Set[A] = {
      val builder = cols.Set.newBuilder[A]
      put(builder.+=(_))
      builder.result()
    }
  }

  case object Vector extends CollectionTag[Vector] {
    override def name: String = "Vector"
    override def iterator[A](c: Vector[A]): Iterator[A] = c.iterator

    override def build[A](put: (A => Unit) => Unit): Vector[A] = {
      val builder = cols.Vector.newBuilder[A]
      put(builder.+=(_))
      builder.result()
    }
  }
}
