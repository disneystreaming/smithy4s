/*
 *  Copyright 2021 Disney Streaming
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

sealed trait Primitive[T] {
  final def schema(shapeId: ShapeId): Schema[T] =
    Schema.PrimitiveSchema(shapeId, Hints.empty, this)
  final def schema(namespace: String, name: String): Schema[T] =
    this.schema(ShapeId(namespace, name))
}

object Primitive {

  case object PShort extends Primitive[Short]
  case object PInt extends Primitive[Int]
  case object PFloat extends Primitive[Float]
  case object PLong extends Primitive[Long]
  case object PDouble extends Primitive[Double]
  case object PBigInt extends Primitive[BigInt]
  case object PBigDecimal extends Primitive[BigDecimal]

  case object PBoolean extends Primitive[Boolean]
  case object PString extends Primitive[String]
  case object PUUID extends Primitive[java.util.UUID]
  case object PByte extends Primitive[Byte]
  case object PBlob extends Primitive[ByteArray]
  case object PDocument extends Primitive[Document]
  case object PTimestamp extends Primitive[Timestamp]
  case object PUnit extends Primitive[Unit]

  def deriving[F[_]](implicit
      short: F[Short],
      int: F[Int],
      float: F[Float],
      long: F[Long],
      double: F[Double],
      bigint: F[BigInt],
      bigdecimal: F[BigDecimal],
      boolean: F[Boolean],
      string: F[String],
      uuid: F[java.util.UUID],
      byte: F[Byte],
      blob: F[ByteArray],
      document: F[Document],
      timestamp: F[Timestamp],
      unit: F[Unit]
  ): PolyFunction[Primitive, F] = new PolyFunction[Primitive, F] {
    def apply[T](prim: Primitive[T]): F[T] = prim match {
      case PShort      => short
      case PInt        => int
      case PFloat      => float
      case PLong       => long
      case PDouble     => double
      case PBigInt     => bigint
      case PBigDecimal => bigdecimal
      case PBoolean    => boolean
      case PString     => string
      case PUUID       => uuid
      case PByte       => byte
      case PBlob       => blob
      case PDocument   => document
      case PTimestamp  => timestamp
      case PUnit       => unit
    }
  }

}
