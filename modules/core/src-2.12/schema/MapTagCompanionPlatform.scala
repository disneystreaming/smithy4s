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

import scala.collection.immutable.ListMap

trait MapTagCompanionPlatform {
  type SeqMapType[K, V] = ListMap[K, V]

  case object SeqMapTag extends MapTag[ListMap] {
    override def name: String = "ListMap"

    override def iterator[K, V](c: ListMap[K, V]): Iterator[(K, V)] =
      c.iterator

    override def build[K, V](
        put: ((K, V) => Unit) => Unit
    ): ListMap[K, V] = {
      val builder = ListMap.newBuilder[K, V]
      put((k, v) => builder += (k -> v))
      builder.result()
    }

    override def toScalaMap[K, V](c: ListMap[K, V]): Map[K, V] = c

    override def isEmpty[K, V](c: ListMap[K, V]): Boolean = c.isEmpty

    override def get[K, V](map: ListMap[K, V], key: K): Option[V] =
      map.get(key)
  }
}
