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

import scala.collection.immutable.SeqMap

trait MapTagCompanionPlatform {
  type SeqMapType[K, V] = SeqMap[K, V]

  case object SeqMapTag extends MapTag[SeqMap] {
    override def name: String = "SeqMap"

    override def iterator[K, V](c: SeqMap[K, V]): Iterator[(K, V)] =
      c.iterator

    override def build[K, V](
        put: ((K, V) => Unit) => Unit
    ): SeqMap[K, V] = {
      val builder = SeqMap.newBuilder[K, V]
      put((k, v) => builder += (k -> v))
      builder.result()
    }

    override def toScalaMap[K, V](c: SeqMap[K, V]): Map[K, V] = c

    override def isEmpty[K, V](c: SeqMap[K, V]): Boolean = c.isEmpty

    override def get[K, V](map: SeqMap[K, V], key: K): Option[V] =
      map.get(key)
  }
}
