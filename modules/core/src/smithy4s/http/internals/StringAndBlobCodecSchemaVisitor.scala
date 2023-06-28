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
package http
package internals

import smithy4s.http.HttpMediaType

import schema._
import smithy4s.schema.Primitive._
import java.nio.charset.StandardCharsets

import StringAndBlobCodecSchemaVisitor._

private[http] object StringAndBlobCodecSchemaVisitor {

  trait SimpleCodec[A] extends CodecAPI.Codec[A] { self =>
    def mediaType: HttpMediaType
    def fromBytes(bytes: Array[Byte]): A
    def toBytes(a: A): Array[Byte]
    def decode(blob: Blob): Either[PayloadError, A] = Right(
      fromBytes(blob.toArray)
    )
    def encode(a: A): Blob = Blob(toBytes(a))
    override def imap[B](to: A => B, from: B => A): SimpleCodec[B] =
      new SimpleCodec[B] {
        def mediaType: HttpMediaType = self.mediaType
        def fromBytes(bytes: Array[Byte]): B = to(self.fromBytes(bytes))
        def toBytes(b: B): Array[Byte] = self.toBytes(from(b))
      }
    override def xmap[B](
        to: A => Either[ConstraintError, B],
        from: B => A
    ): SimpleCodec[B] = new SimpleCodec[B] {
      def mediaType: HttpMediaType = self.mediaType
      def fromBytes(bytes: Array[Byte]): B = to(self.fromBytes(bytes)) match {
        case Right(value) => value
        case Left(e)      => throw e
      }
      def toBytes(b: B): Array[Byte] = self.toBytes(from(b))
    }
  }

  sealed trait CodecResult[A]
  case class BodyCodecResult[A](
      codec: CodecAPI.Codec[A]
  ) extends CodecResult[A]

  case class NoCodecResult[A]() extends CodecResult[A]

  object CodecResult {

    implicit val invariantInstance: smithy4s.capability.Invariant[CodecResult] =
      new smithy4s.capability.Invariant[CodecResult] {
        def imap[A, B](
            fa: CodecResult[A]
        )(to: A => B, from: B => A): CodecResult[B] = fa match {
          case BodyCodecResult(codec) => BodyCodecResult(codec.imap(to, from))
          case NoCodecResult()        => NoCodecResult()
        }
        def xmap[A, B](
            fa: CodecResult[A]
        )(to: A => Either[ConstraintError, B], from: B => A): CodecResult[B] =
          fa match {
            case BodyCodecResult(codec) => BodyCodecResult(codec.xmap(to, from))
            case NoCodecResult()        => NoCodecResult()
          }
      }

  }

  def noop[A]: CodecResult[A] = NoCodecResult[A]()

}

private[http] class StringAndBlobCodecSchemaVisitor
    extends SchemaVisitor.Default[CodecResult] { self =>

  override def default[A]: CodecResult[A] = noop

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): CodecResult[P] = tag match {
    case PString =>
      BodyCodecResult(stringCodec(hints))

    case PBlob =>
      val maybeMediaTypeHint = smithy.api.MediaType.hint.unapply(hints)
      BodyCodecResult {
        new SimpleCodec[ByteArray] {
          val mediaType: HttpMediaType = HttpMediaType(
            maybeMediaTypeHint
              .map(_.value)
              .getOrElse("application/octet-stream")
          )

          def fromBytes(bytes: Array[Byte]): ByteArray = ByteArray(bytes)

          def toBytes(a: ByteArray): Array[Byte] = a.array
        }
      }
    case PTimestamp | PUUID | PBigInt | PBoolean | PLong | PShort | PDocument |
        PByte | PDouble | PFloat | PBigDecimal | PInt =>
      noop
  }

  private[internals] def stringCodec(hints: Hints): SimpleCodec[String] = {
    val maybeMediaTypeHint = smithy.api.MediaType.hint.unapply(hints)
    new SimpleCodec[String] {
      val mediaType: HttpMediaType = HttpMediaType(
        maybeMediaTypeHint
          .map(_.value)
          .getOrElse("text/plain")
      )

      def fromBytes(bytes: Array[Byte]): String =
        new String(bytes, StandardCharsets.UTF_8)

      def toBytes(a: String): Array[Byte] = a.getBytes()
    }
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): CodecResult[E] = {
    tag match {
      case EnumTag.StringEnum =>
        BodyCodecResult {
          stringCodec(hints).imap(
            str =>
              values
                .find(_.stringValue == str)
                .getOrElse(
                  throw new PayloadError(
                    PayloadPath.root,
                    s"expected one of ${values.mkString(",")}",
                    s"Unknown enum value $str"
                  )
                )
                .value,
            e => total(e).stringValue
          )
        }

      case EnumTag.IntEnum => noop
    }
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): CodecResult[B] =
    CodecResult.invariantInstance.biject(apply(schema))(bijection)

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): CodecResult[B] =
    CodecResult.invariantInstance
      .xmap(apply(schema))(refinement.asFunction, refinement.from)

  override def nullable[A](
      schema: Schema[A]
  ): CodecResult[Option[A]] =
    self(schema) match {
      case BodyCodecResult(codec) =>
        BodyCodecResult {
          new CodecAPI.Codec[Option[A]] {
            def mediaType: HttpMediaType = codec.mediaType

            def decode(blob: Blob): Either[PayloadError, Option[A]] =
              if (blob.isEmpty) Right(None)
              else codec.decode(blob).map(a => Some(a))
            def encode(value: Option[A]): Blob = value match {
              case None        => Blob.empty
              case Some(value) => codec.encode(value)
            }
          }

        }
      case NoCodecResult() => NoCodecResult()
    }

}
