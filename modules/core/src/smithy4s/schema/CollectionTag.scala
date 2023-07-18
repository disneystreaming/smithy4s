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

import scala.reflect.ClassTag

sealed trait CollectionTag[C[_]] {
  def name: String

  def iterator[A](c: C[A]): Iterator[A]
  def build[A](put: (A => Unit) => Unit): C[A]

  def fromIterator[A](it: Iterator[A]): C[A] = build(put => it.foreach(put(_)))
  def isEmpty[A](c: C[A]): Boolean
  def empty[A]: C[A] = build(_ => ())
}

object CollectionTag {
  import scala.collection.compat.{immutable => cols}
  import scala.collection.{mutable => mut}

  case object ListTag extends CollectionTag[List] {
    override def name: String = "List"

    override def iterator[A](c: List[A]): Iterator[A] = c.iterator

    override def build[A](put: (A => Unit) => Unit): List[A] = {
      val builder = mut.ListBuffer.newBuilder[A]
      put(builder += (_))
      builder.result().toList
    }

    override def isEmpty[A](c: List[A]): Boolean = c.isEmpty

  }

  case object SetTag extends CollectionTag[Set] {
    override def name: String = "Set"
    override def iterator[A](c: Set[A]): Iterator[A] = c.iterator

    override def build[A](put: (A => Unit) => Unit): Set[A] = {
      val builder = Set.newBuilder[A]
      put(builder.+=(_))
      builder.result()
    }

    override def isEmpty[A](c: Set[A]): Boolean = c.isEmpty
  }

  case object VectorTag extends CollectionTag[Vector] {
    override def name: String = "Vector"
    override def iterator[A](c: Vector[A]): Iterator[A] = c.iterator

    override def build[A](put: (A => Unit) => Unit): Vector[A] = {
      val builder = Vector.newBuilder[A]
      put(builder.+=(_))
      builder.result()
    }

    override def isEmpty[A](c: Vector[A]): Boolean = c.isEmpty
  }

  case object IndexedSeqTag extends CollectionTag[IndexedSeq] {

    override def name: String = "IndexedSeq"
    override def iterator[A](c: IndexedSeq[A]): Iterator[A] = c.iterator

    override def build[A](put: (A => Unit) => Unit): Vector[A] = {
      val builder = Vector.newBuilder[A]
      put(builder.+=(_))
      builder.result()
    }

    override def isEmpty[A](c: IndexedSeq[A]): Boolean = c.isEmpty

    /**
      * Returns a builder that may be able to store the elements in an unboxed
      * fashion
      */
    private[smithy4s] def compactBuilder[A](
        schema: Schema[A]
    ): ((A => Unit) => Unit) => IndexedSeq[A] =
      schema.compile(CTSchemaVisitor) match {
        case Some(ct) => { (put: (A => Unit) => Unit) =>
          var as = Array.ofDim(8)(ct)
          var i = 0
          put { a =>
            if (i == as.length) as = copyOf(as, i << 1)
            as(i) = a
            i += 1
          }
          if (i < as.length) as = copyOf(as, i)
          cols.ArraySeq.unsafeWrapArray(as)
        }
        case None => { (put: (A => Unit) => Unit) =>
          val builder = IndexedSeq.newBuilder[A]
          put(builder.+=(_))
          builder.result()
        }
      }
  }

  private[this] def copyOf[A](original: Array[A], newLength: Int): Array[A] =
    ((original: @unchecked) match {
      case x: Array[AnyRef]  => java.util.Arrays.copyOf(x, newLength)
      case x: Array[Long]    => java.util.Arrays.copyOf(x, newLength)
      case x: Array[Double]  => java.util.Arrays.copyOf(x, newLength)
      case x: Array[Int]     => java.util.Arrays.copyOf(x, newLength)
      case x: Array[Float]   => java.util.Arrays.copyOf(x, newLength)
      case x: Array[Short]   => java.util.Arrays.copyOf(x, newLength)
      case x: Array[Char]    => java.util.Arrays.copyOf(x, newLength)
      case x: Array[Byte]    => java.util.Arrays.copyOf(x, newLength)
      case x: Array[Boolean] => java.util.Arrays.copyOf(x, newLength)
    }).asInstanceOf[Array[A]]

  private[this] type MaybeCT[A] = Option[ClassTag[A]]

  /**
   * Retrieves a ClassTag whenever possible.
   */
  // format: off
  private[this] object CTSchemaVisitor extends SchemaVisitor[MaybeCT]{
    val primitiveCT = Primitive.deriving[ClassTag]
    def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]): MaybeCT[P] = Some(primitiveCT(tag))
    def collection[C[_], A](shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: Schema[A]): MaybeCT[C[A]] = tag match {
      case ListTag => Some(implicitly[ClassTag[List[A]]])
      case SetTag => Some(implicitly[ClassTag[Set[A]]])
      case VectorTag => Some(implicitly[ClassTag[Vector[A]]])
      case IndexedSeqTag => Some(implicitly[ClassTag[IndexedSeq[A]]])
    }
    def map[K, V](shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]): MaybeCT[Map[K,V]] = Some(implicitly[ClassTag[Map[K, V]]])
    def enumeration[E](shapeId: ShapeId, hints: Hints, tag: EnumTag, values: List[EnumValue[E]], total: E => EnumValue[E]): MaybeCT[E] = None
    def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[Field[S, _]], make: IndexedSeq[Any] => S): MaybeCT[S] = None
    def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[Alt[U, _]], dispatch: Alt.Dispatcher[U]): MaybeCT[U] = None
    def biject[A, B](schema: Schema[A], bijection: Bijection[A, B]): MaybeCT[B] = {
      if (bijection.isInstanceOf[Newtype.Make[_, _]]) apply(schema).asInstanceOf[MaybeCT[B]]
      else None
    }
    def refine[A, B](schema: Schema[A], refinement: Refinement[A,B]): MaybeCT[B] = None
    def lazily[A](suspend: Lazy[Schema[A]]): MaybeCT[A] = None
    def option[A](schema: Schema[A]) = Some(implicitly[ClassTag[Option[A]]])
  }
  // format: off
}
