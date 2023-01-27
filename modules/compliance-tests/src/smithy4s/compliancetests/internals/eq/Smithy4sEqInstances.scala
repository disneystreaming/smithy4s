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

package smithy4s.compliancetests.internals.eq

import cats.implicits.catsSyntaxEq
import cats.implicits.toContravariantOps
import cats.kernel.Eq
import cats.kernel.instances.StaticMethods
import smithy4s.ByteArray
import smithy4s.Document
import smithy4s.Timestamp

trait Smithy4sEqInstances {
  implicit def arrayEq[A: Eq]: Eq[Array[A]] = (x: Array[A], y: Array[A]) =>
    x.zip(y).forall { case (a, b) => a === b }

  implicit def indexedSeqEq[A: Eq]: Eq[IndexedSeq[A]] =
    (xs: IndexedSeq[A], ys: IndexedSeq[A]) =>
      if (xs eq ys) true
      else StaticMethods.iteratorEq(xs.iterator, ys.iterator)

  implicit val byteArrayEq: Eq[ByteArray] = (x: ByteArray, y: ByteArray) =>
    Eq[Array[Byte]].contramap[ByteArray](_.array).eqv(x, y)
  implicit val documentEq: Eq[Document] = Eq[String].contramap(_.show)
  implicit val timeStampEq: Eq[Timestamp] =
    Eq[Long].contramap(_.epochSecond)

}
object Smithy4sEqInstances extends Smithy4sEqInstances
