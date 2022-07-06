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

  }

  case object SetTag extends CollectionTag[Set] {
    override def name: String = "Set"
    override def iterator[A](c: Set[A]): Iterator[A] = c.iterator

    override def build[A](put: (A => Unit) => Unit): Set[A] = {
      val builder = Set.newBuilder[A]
      put(builder.+=(_))
      builder.result()
    }
  }

  case object VectorTag extends CollectionTag[Vector] {
    override def name: String = "Vector"
    override def iterator[A](c: Vector[A]): Iterator[A] = c.iterator

    override def build[A](put: (A => Unit) => Unit): Vector[A] = {
      val builder = Vector.newBuilder[A]
      put(builder.+=(_))
      builder.result()
    }
  }

  case object IndexedSeqTag extends CollectionTag[IndexedSeq] {

    override def name: String = "IndexedSeq"
    override def iterator[A](c: IndexedSeq[A]): Iterator[A] = c.iterator

    override def build[A](put: (A => Unit) => Unit): Vector[A] = {
      val builder = Vector.newBuilder[A]
      put(builder.+=(_))
      builder.result()
    }

    /**
      * Returns a builder that may be able to store the elements in an unboxed
      * fashion
      */
    private[smithy4s] def compactBuilder[A](
        schema: Schema[A]
    ): ((A => Unit) => Unit) => IndexedSeq[A] =
      schema.compile(CTSchemaVisitor) match {
        case Some(ct) => { (put: (A => Unit) => Unit) =>
          val builder = cols.ArraySeq.newBuilder(ct)
          put(builder.+=(_))
          builder.result()
        }
        case None => { (put: (A => Unit) => Unit) =>
          val builder = IndexedSeq.newBuilder[A]
          put(builder.+=(_))
          builder.result()
        }
      }
  }

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
    def enumeration[E](shapeId: ShapeId, hints: Hints, values: List[EnumValue[E]], total: E => EnumValue[E]): MaybeCT[E] = None
    def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[SchemaField[S, _]], make: IndexedSeq[Any] => S): MaybeCT[S] = None
    def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[SchemaAlt[U, _]], dispatch: U => Alt.SchemaAndValue[U, _]): MaybeCT[U] = None
    def biject[A, B](schema: Schema[A], to: A => B, from: B => A): MaybeCT[B] = {
      if (to.isInstanceOf[Newtype.Make[_, _]]) apply(schema).asInstanceOf[MaybeCT[B]]
      else None
    }
    def surject[A, B](schema: Schema[A], to: Refinement[A,B], from: B => A): MaybeCT[B] = None
    def lazily[A](suspend: Lazy[Schema[A]]): MaybeCT[A] = None
  }
  // format: off
}
