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
package schema

trait MapTag[C[_, _]] {
  def name: String

  def iterator[K, V](c: C[K, V]): Iterator[(K, V)]
  def toScalaMap[K, V](c: C[K, V]): Map[K, V] = iterator(c).toMap
  def build[K, V](put: ((K, V) => Unit) => Unit): C[K, V]
  def fromIterator[K, V](it: Iterator[(K, V)]): C[K, V] =
    build(put => it.foreach(x => put(x._1, x._2)))
  def fromScalaMap[K, V](map: Map[K, V]): C[K, V] =
    build(put => map.foreach(x => put(x._1, x._2)))

  def isEmpty[K, V](c: C[K, V]): Boolean
  def empty[K, V]: C[K, V] = build(_ => ())
  def get[K, V](map: C[K, V], key: K): Option[V]
}

object MapTag extends MapTagCompanionPlatform {
  case object ScalaMapTag extends MapTag[Map] {
    override def name: String = "Map"

    override def iterator[K, V](c: Map[K, V]): Iterator[(K, V)] = c.iterator

    override def build[K, V](put: ((K, V) => Unit) => Unit): Map[K, V] = {
      val builder = Map.newBuilder[K, V]
      put((k, v) => builder += (k -> v))
      builder.result()
    }

    override def toScalaMap[K, V](c: Map[K, V]): Map[K, V] = c

    override def fromScalaMap[K, V](map: Map[K, V]): Map[K, V] = map

    override def isEmpty[K, V](c: Map[K, V]): Boolean = c.isEmpty

    override def get[K, V](map: Map[K, V], key: K): Option[V] = map.get(key)
  }
}
