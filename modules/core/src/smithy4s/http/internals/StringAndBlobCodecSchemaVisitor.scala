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
import smithy.api.HttpPayload
import java.nio.ByteBuffer

private[smithy4s] object StringAndBlobCodecSchemaVisitor {

  trait SimpleCodec[A] { self =>
    def mediaType: HttpMediaType
    def fromBytes(bytes: Array[Byte]): A
    def toBytes(a: A): Array[Byte]
    def imap[B](to: A => B, from: B => A): SimpleCodec[B] = new SimpleCodec[B] {
      def mediaType: HttpMediaType = self.mediaType
      def fromBytes(bytes: Array[Byte]): B = to(self.fromBytes(bytes))
      def toBytes(b: B): Array[Byte] = self.toBytes(from(b))
    }
    def xmap[B](
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
  case class SimpleCodecResult[A](simpleCodec: SimpleCodec[A])
      extends CodecResult[A]
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
          case SimpleCodecResult(simpleCodec) =>
            SimpleCodecResult(simpleCodec.imap(to, from))
          case BodyCodecResult(codec) => BodyCodecResult(codec.imap(to, from))
          case NoCodecResult()        => NoCodecResult()
        }
        def xmap[A, B](
            fa: CodecResult[A]
        )(to: A => Either[ConstraintError, B], from: B => A): CodecResult[B] =
          fa match {
            case SimpleCodecResult(simpleCodec) =>
              SimpleCodecResult(simpleCodec.xmap(to, from))
            case BodyCodecResult(codec) => BodyCodecResult(codec.xmap(to, from))
            case NoCodecResult()        => NoCodecResult()
          }
      }

  }

  def noop[A]: CodecResult[A] = NoCodecResult[A]()

}

private[smithy4s] class StringAndBlobCodecSchemaVisitor
    extends SchemaVisitor.Default[CodecResult] {

  override def default[A]: CodecResult[A] = noop

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): CodecResult[P] = tag match {
    case PString =>
      val maybeMediaTypeHint = smithy.api.MediaType.hint.unapply(hints)
      SimpleCodecResult {
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

    case PBlob =>
      val maybeMediaTypeHint = smithy.api.MediaType.hint.unapply(hints)
      SimpleCodecResult {
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
    case PTimestamp | PUUID | PBigInt | PUnit | PBoolean | PLong | PShort |
        PDocument | PByte | PDouble | PFloat | PBigDecimal | PInt =>
      noop
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): CodecResult[S] = {
    def processField[A](field: SchemaField[S, A]): CodecResult[S] = {
      val folder = new Field.FolderK[Schema, S, CodecResult]() {
        override def onRequired[AA](
            label: String,
            instance: Schema[AA],
            get: S => AA
        ): CodecResult[AA] = apply(instance)
        override def onOptional[AA](
            label: String,
            instance: Schema[AA],
            get: S => Option[AA]
        ): CodecResult[Option[AA]] = apply(instance) match {
          case SimpleCodecResult(simpleCodec) =>
            SimpleCodecResult(new SimpleCodec[Option[AA]] {
              def mediaType: HttpMediaType = simpleCodec.mediaType

              def fromBytes(bytes: Array[Byte]): Option[AA] =
                if (bytes.isEmpty) None else Some(simpleCodec.fromBytes(bytes))

              def toBytes(a: Option[AA]): Array[Byte] = a match {
                case Some(value) => simpleCodec.toBytes(value)
                case None        => Array.emptyByteArray
              }
            })
          case BodyCodecResult(_) => NoCodecResult[Option[AA]]()
          case NoCodecResult()    => NoCodecResult[Option[AA]]()
        }
      }
      val instance: CodecResult[A] = field.foldK(folder)

      instance match {
        case SimpleCodecResult(simpleCodec) =>
          BodyCodecResult(new CodecAPI.Codec[S] {
            def mediaType: HttpMediaType = simpleCodec.mediaType

            def decodeFromByteArrayPartial(
                bytes: Array[Byte]
            ): Either[PayloadError, BodyPartial[S]] = {
              val a = simpleCodec.fromBytes(bytes)
              Right(BodyPartial { map =>
                def access(l: String) = if (l == field.label) a else map(l)
                val vec = Vector.newBuilder[Any]
                fields.foreach { f =>
                  vec += access(f.label)
                }
                make(vec.result())
              })
            }

            def decodeFromByteBufferPartial(
                bytes: ByteBuffer
            ): Either[PayloadError, BodyPartial[S]] = {
              val arr: Array[Byte] =
                Array.ofDim[Byte](bytes.remaining())
              val _ = bytes.get(arr)
              decodeFromByteArrayPartial(arr)
            }

            def writeToArray(value: S): Array[Byte] =
              simpleCodec.toBytes(field.get(value))
          })
        case BodyCodecResult(_) => noop[S]
        case NoCodecResult()    => noop[S]
      }
    }

    fields
      .find(p => p.instance.hints.get(HttpPayload).isDefined)
      .map { field => processField(field) }
      .getOrElse(noop[S])

  }

  override def biject[A, B](
      schema: Schema[A],
      to: A => B,
      from: B => A
  ): CodecResult[B] =
    CodecResult.invariantInstance.imap(apply(schema))(to, from)

  override def surject[A, B](
      schema: Schema[A],
      to: Refinement[A, B],
      from: B => A
  ): CodecResult[B] =
    CodecResult.invariantInstance.xmap(apply(schema))(to.asFunction, from)

}
