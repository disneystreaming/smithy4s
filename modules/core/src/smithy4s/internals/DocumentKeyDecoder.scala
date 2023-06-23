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

package smithy4s.internals

import java.util.Base64
import java.util.UUID

import smithy4s._
import smithy4s.Document._
import smithy4s.schema.EnumValue
import smithy4s.schema.Primitive
import smithy4s.schema.Primitive._
import smithy4s.schema.SchemaVisitor
import smithy4s.schema.EnumTag

trait DocumentKeyDecoder[A] { self =>
  def apply(v: Document): Either[DocumentKeyDecoder.DecodeError, A] =
    try { Right(unsafeDecode(v)) }
    catch { case ex: DocumentKeyDecoder.DecodeError => Left(ex) }

  def unsafeDecode(v: Document): A

  def map[B](f: A => B): DocumentKeyDecoder[B] = new DocumentKeyDecoder[B] {
    def unsafeDecode(v: Document): B = f(self.unsafeDecode(v))
  }
}

object DocumentKeyDecoder {
  case class DecodeError(expectedType: String)
      extends RuntimeException("Cannot decode a key.", null)

  type OptDocumentKeyDecoder[A] = Option[DocumentKeyDecoder[A]]
  val trySchemaVisitor: SchemaVisitor[OptDocumentKeyDecoder] =
    new SchemaVisitor.Default[OptDocumentKeyDecoder] {
      def default[A]: OptDocumentKeyDecoder[A] = None

      def from[A](
          expectedType: String
      )(f: PartialFunction[Document, A]): OptDocumentKeyDecoder[A] =
        Some { doc =>
          if (f.isDefinedAt(doc)) f(doc)
          else throw DecodeError(expectedType)
        }
      def fromUnsafe[A](
          expectedType: String
      )(f: PartialFunction[Document, A]): OptDocumentKeyDecoder[A] =
        Some { doc =>
          if (f.isDefinedAt(doc)) {
            f(doc)
          } else {
            throw DecodeError(expectedType)
          }
        }

      override def primitive[P](
          shapeId: ShapeId,
          hints: Hints,
          tag: Primitive[P]
      ): OptDocumentKeyDecoder[P] = {
        val shortDesc = tag.schema(shapeId).compile(SchemaDescription)
        tag match {
          case PShort =>
            from(shortDesc) {
              case FlexibleNumber(bd) if bd.isValidShort => bd.shortValue
            }
          case PString => from(shortDesc) { case DString(value) => value }
          case PFloat =>
            from(shortDesc) { case FlexibleNumber(bd) =>
              bd.toFloat
            }
          case PDouble =>
            from(shortDesc) {
              case FlexibleNumber(bd) if bd.isDecimalDouble => bd.toDouble
            }

          case PTimestamp => None // not sure because we can encode

          case PBlob =>
            fromUnsafe(shortDesc) { case DString(string) =>
              ByteArray(Base64.getDecoder().decode(string))
            }
          case PBigInt =>
            from(shortDesc) {
              case FlexibleNumber(bd) if bd.isWhole => bd.toBigInt
            }
          case PUUID =>
            fromUnsafe(shortDesc) { case DString(string) =>
              UUID.fromString(string)
            }
          case PInt =>
            from(shortDesc) {
              case FlexibleNumber(bd) if bd.isValidInt => bd.intValue
            }
          case PBigDecimal =>
            from(shortDesc) { case FlexibleNumber(bd) =>
              bd
            }
          case PBoolean =>
            from(shortDesc) {
              case DBoolean(value)  => value
              case DString("true")  => true
              case DString("false") => false
            }
          case PLong =>
            from(shortDesc) {
              case FlexibleNumber(bd) if bd.isValidLong => bd.longValue
            }
          case PByte =>
            from(shortDesc) {
              case FlexibleNumber(bd) if bd.isValidByte => bd.toByte
            }

          case PDocument => None
        }
      }
      override def enumeration[E](
          shapeId: ShapeId,
          hints: Hints,
          tag: EnumTag,
          values: List[EnumValue[E]],
          total: E => EnumValue[E]
      ): OptDocumentKeyDecoder[E] = {
        val fromName = values.map(e => e.stringValue -> e.value).toMap
        from(s"value in [${fromName.keySet.mkString(", ")}]") {
          case DString(value) if fromName.contains(value) => fromName(value)
        }
      }

      override def biject[A, B](
          schema: Schema[A],
          bijection: Bijection[A, B]
      ): OptDocumentKeyDecoder[B] =
        apply(schema).map(_.map(bijection.to))

      override def refine[A, B](
          schema: Schema[A],
          to: Refinement[A, B]
      ): OptDocumentKeyDecoder[B] =
        apply(schema).map(_.map(to.asThrowingFunction))
    }

  object FlexibleNumber {
    def unapply(doc: Document): Option[BigDecimal] = doc match {
      case DNumber(value) => Some(value)
      case DString(value) =>
        try { Some(BigDecimal(value)) }
        catch { case _: Throwable => None }
      case _ => None
    }
  }
}
