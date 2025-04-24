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

package smithy4s.dynamic

package object internals {

  private[internals] type DynData = Any
  private[internals] type DynStruct = Array[DynData]
  private[internals] type DynAlt = (Int, DynData)

  private[internals] def recursiveVertices[A](map: Map[A, Set[A]]): Set[A] = {
    import scala.collection.mutable.{Set => MSet}
    import scala.collection.immutable.ListSet
    val provenRecursive: MSet[A] = MSet.empty
    val provenNotRecursive: MSet[A] = MSet.empty

    def crawl(key: A, seen: ListSet[A]): Unit = {
      if (provenNotRecursive(key)) ()
      else if (seen(key)) {
        // dropping elements that don't belong to the "recursive path"
        provenRecursive ++= seen.dropWhile(_ != key)
      } else
        map.get(key) match {
          case None => ()
          case Some(values) =>
            values.foreach(crawl(_, seen + key))
            if (!provenRecursive(key)) provenNotRecursive += key
        }
    }

    map.keySet.foreach(crawl(_, ListSet.empty))
    provenRecursive.toSet
  }
}
