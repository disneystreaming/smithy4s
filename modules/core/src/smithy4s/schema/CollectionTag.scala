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

sealed trait CollectionTag[C[_], A] {
  def iterator(c: C[A]): Iterator[A]
  def build(put: (A => Unit) => Unit): C[A]

  def fromIterator(it: Iterator[A]): C[A] = build(put => it.foreach(put(_)))
  def empty: C[A] = build(_ => ())
}

object CollectionTag {
  def list[A]: CollectionTag[List, A] = new CollectionTag[List, A] {

    override def iterator(c: List[A]): Iterator[A] = c.iterator

    override def build(put: (A => Unit) => Unit): List[A] = {
      val builder = List.newBuilder[A]
      put(builder.+=(_))
      builder.result()
    }

  }
  def set[A]: CollectionTag[Set, A] = new CollectionTag[Set, A] {

    override def iterator(c: Set[A]): Iterator[A] = c.iterator

    override def build(put: (A => Unit) => Unit): Set[A] = {
      val builder = Set.newBuilder[A]
      put(builder.+=(_))
      builder.result()
    }

  }
}
