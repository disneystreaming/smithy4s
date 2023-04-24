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

package smithy4s.interopcats.instances

import cats.{Eq, Hash}
import smithy4s.{ByteArray, ShapeId, Timestamp}
import smithy4s.kinds.PolyFunction
import smithy4s.schema.Primitive

trait HashInstances {

  implicit val byteArrayHash: Hash[ByteArray] =
    new Hash[ByteArray] {
      override def hash(x: ByteArray): Int = x.array.hashCode()

      override def eqv(x: ByteArray, y: ByteArray): Boolean =
        Eq[ByteArray].eqv(x, y)
    }
  implicit val documentHash: Hash[smithy4s.Document] =
    Hash.fromUniversalHashCode
  implicit val shapeIdHash: Hash[ShapeId] = Hash.fromUniversalHashCode
  implicit val timeStampHash: Hash[Timestamp] = Hash.fromUniversalHashCode
  val primHashPf: PolyFunction[Primitive, Hash] = Primitive.deriving[Hash]

}

object HashInstances extends HashInstances
