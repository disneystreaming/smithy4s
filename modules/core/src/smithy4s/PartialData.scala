/*
 *  Copyright 2021-2023 Disney Streaming
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

import scala.collection.compat.immutable.ArraySeq

/**
  * Data structure that can hold either the totality or a subset of a larger piece of data.
  *
  * This can be used to reconcile bits of data that are coming from several locations,
  * or to send a piece of data towards different locations.
  *
  * For instance :
  *
  * {{{
  * structure AB {
  *   @httpPayload
  *   @required
  *   a: String,
  *
  *   @httpHeader("X-B")
  *   @required
  *   b: String
  * }
  * }}}
  *
  * translates to this case class {{{ case class AB(a: String, b: String) }}}.
  *
  * However, codec derivation (performed by the SchemaVisitor mechanism in Smithy4s), does not let us easily solve for
  * problem such as http message decoding, because of the notion of priority : the http body of a message should not
  * be decoded if the metadata is not fully decoded first.
  *
  * In order to solve for this, the [[PartialData]] type allows to momentarily store a subset of the fields of a case
  * class so that it can be reconciled with other pieces of [[PartialData]] later on.
  */
sealed trait PartialData[A] {
  def map[B](f: A => B): PartialData[B]
}
// format: off
object PartialData {
  final case class Total[A](a: A) extends PartialData[A] {
    def map[B](f: A => B): PartialData[B] = Total(f(a))
  }
  final case class Partial[A](indexes: IndexedSeq[Int], partialData: IndexedSeq[Any], make: IndexedSeq[Any] => A) extends PartialData[A] {
    def map[B](f: A => B): PartialData[B] = Partial(indexes, partialData, make andThen f)
  }

  /**
    * Reconciles bits of partial data (typically retrieved from various parts of a message)
    * into a single piece of data. It is the responsibility of the caller to ensure that
    * the individual pieces can be reconciled into the full data.
    */
  def unsafeReconcile[A](pieces: PartialData[A]*): A = {
    pieces.collectFirst {
      case Total(a) => a
    }.getOrElse {
      val allPieces = pieces.asInstanceOf[Seq[PartialData.Partial[A]]]
      var totalSize = 0
      allPieces.foreach(totalSize += _.indexes.size)
      val array = Array.fill[Any](totalSize)(null)
      var make : IndexedSeq[Any] => A = null
      allPieces.foreach { case PartialData.Partial(indexes, data, const) =>
        // all the `const` values should be the same, therefore which one is called
        // is an arbitrary choice.
        make = const
        var i = 0
        while(i < data.size) {
          array(indexes(i)) = data(i)
          i += 1
        }
      }
      make(ArraySeq.unsafeWrapArray(array))
    }
  }
}
