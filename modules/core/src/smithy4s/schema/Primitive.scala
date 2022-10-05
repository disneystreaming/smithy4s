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

import smithy4s.http.HttpBinding
import smithy.api.TimestampFormat

sealed trait Primitive[T] {
  final def schema(shapeId: ShapeId): Schema[T] =
    Schema.PrimitiveSchema(shapeId, Hints.empty, this)
  final def schema(namespace: String, name: String): Schema[T] =
    this.schema(ShapeId(namespace, name))
}

object Primitive extends smithy4s.ScalaCompat {

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

  def describe(p: Primitive[_]): String = p match {
    case Primitive.PShort      => "Short"
    case Primitive.PInt        => "Int"
    case Primitive.PFloat      => "Float"
    case Primitive.PLong       => "Long"
    case Primitive.PDouble     => "Double"
    case Primitive.PBigInt     => "BigInt"
    case Primitive.PBigDecimal => "BigDecimal"
    case Primitive.PBoolean    => "Boolean"
    case Primitive.PString     => "String"
    case Primitive.PUUID       => "UUID"
    case Primitive.PByte       => "Byte"
    case Primitive.PBlob       => "Bytes"
    case Primitive.PDocument   => "Document"
    case Primitive.PTimestamp  => "Timestamp"
    case Primitive.PUnit       => "Unit"
  }

  private[smithy4s] def stringParser[A](
      primitive: Primitive[A],
      hints: Hints
  ): Option[String => Option[A]] = {
    primitive match {
      case Primitive.PShort      => Some(_.toShortOption)
      case Primitive.PInt        => Some(_.toIntOption)
      case Primitive.PFloat      => Some(_.toFloatOption)
      case Primitive.PLong       => Some(_.toLongOption)
      case Primitive.PDouble     => Some(_.toDoubleOption)
      case Primitive.PBigInt     => Some(unsafeStringParser(BigInt(_)))
      case Primitive.PBigDecimal => Some(unsafeStringParser(BigDecimal(_)))
      case Primitive.PBoolean    => Some(_.toBooleanOption)
      case Primitive.PByte       => Some(_.toByteOption)
      case Primitive.PString     => Some(s => Some(s))
      case Primitive.PBlob =>
        Some(
          unsafeStringParser(s =>
            ByteArray(java.util.Base64.getDecoder().decode(s))
          )
        )
      case Primitive.PUUID =>
        Some(unsafeStringParser(java.util.UUID.fromString))
      case Primitive.PTimestamp => Some(timestampParser(hints))
      case Primitive.PUnit      => None
      case Primitive.PDocument  => None
    }
  }

  private[smithy4s] def stringWriter[A](
      primitive: Primitive[A],
      hints: Hints
  ): Option[A => String] = {
    primitive match {
      case Primitive.PShort      => Some(_.toString)
      case Primitive.PInt        => Some(_.toString)
      case Primitive.PFloat      => Some(_.toString)
      case Primitive.PLong       => Some(_.toString)
      case Primitive.PDouble     => Some(_.toString)
      case Primitive.PBigInt     => Some(_.toString)
      case Primitive.PBigDecimal => Some(_.toString)
      case Primitive.PBoolean    => Some(_.toString)
      case Primitive.PByte       => Some(_.toString)
      case Primitive.PUUID       => Some(_.toString)
      case Primitive.PString     => Some(identity[String])
      case Primitive.PTimestamp  => Some(timestampWriter(hints))
      case Primitive.PBlob =>
        Some(bytes => java.util.Base64.getEncoder().encodeToString(bytes.array))
      case Primitive.PUnit     => None
      case Primitive.PDocument => None
    }
  }

  private[smithy4s] def timestampFormat(hints: Hints): TimestampFormat = {
    import HttpBinding.Type._
    val tsFormat = hints.get(TimestampFormat)
    val httpBinding = hints.get(HttpBinding).map(_.tpe)
    val bindingFormat = httpBinding.flatMap {
      case HeaderType     => Some(TimestampFormat.HTTP_DATE)
      case PathType       => Some(TimestampFormat.DATE_TIME)
      case QueryType      => Some(TimestampFormat.DATE_TIME)
      case StatusCodeType => None
    }
    tsFormat.orElse(bindingFormat).getOrElse(TimestampFormat.DATE_TIME)
  }

  private def timestampParser(hints: Hints): String => Option[Timestamp] = {
    val finalFormat = timestampFormat(hints)
    Timestamp.parse(_, finalFormat)
  }

  private def timestampWriter(hints: Hints): Timestamp => String = {
    val finalFormat = timestampFormat(hints)
    _.format(finalFormat)
  }

  private def unsafeStringParser[A](f: String => A): String => Option[A] = s =>
    try { Some(f(s)) }
    catch { case scala.util.control.NonFatal(_) => None }
}
