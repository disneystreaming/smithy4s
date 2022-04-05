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
package http
package internals

import smithy.api.HttpPayload
import smithy4s.http.BodyPartial
import smithy4s.http.HttpMediaType
import smithy4s.http.PayloadError
import smithy4s.internals.Hinted

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import schema._
import StringAndBlobCodecSchematic._

private[smithy4s] object StringAndBlobCodecSchematic {

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

  type Result[A] = Hinted[CodecResult, A]

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

  def noop[A]: Result[A] = Hinted.static(NoCodecResult[A]())

}

private[smithy4s] class StringAndBlobCodecSchematic(constraints: Constraints)
    extends Schematic[Result]
    with StubSchematic[Result] {

  def default[A]: Result[A] = noop[A]

  override def string: Result[String] = Hinted[CodecResult]
    .onHintOpt[smithy.api.MediaType, String] { maybeMediaTypeHint =>
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
    }
    .validatedI(constraints.checkString)

  override def bytes: Result[ByteArray] =
    Hinted[CodecResult].onHintOpt[smithy.api.MediaType, ByteArray] {
      maybeMediaTypeHint =>
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
    }

  override def struct[S](fields: Vector[Field[Result, S, _]])(
      const: Vector[Any] => S
  ): Result[S] = {
    type CodecResultOpt[A] = CodecResult[Option[A]]
    def transformOpt = new PolyFunction[CodecResult, CodecResultOpt] {
      def apply[A](fa: CodecResult[A]): CodecResultOpt[A] = fa match {
        case SimpleCodecResult(simpleCodec) =>
          SimpleCodecResult(new SimpleCodec[Option[A]] {
            def mediaType: HttpMediaType = simpleCodec.mediaType

            def fromBytes(bytes: Array[Byte]): Option[A] =
              if (bytes.isEmpty) None else Some(simpleCodec.fromBytes(bytes))

            def toBytes(a: Option[A]): Array[Byte] = a match {
              case Some(value) => simpleCodec.toBytes(value)
              case None        => Array.emptyByteArray
            }
          })
        case BodyCodecResult(_) => NoCodecResult[Option[A]]()
        case NoCodecResult()    => NoCodecResult[Option[A]]()
      }
    }

    def processField[A](field: Field[Result, S, A]): Result[S] = {
      val instance: Result[A] =
        field.instanceA(Hinted.wrapK(transformOpt))

      instance.get match {
        case SimpleCodecResult(simpleCodec) =>
          Hinted.static {
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
                  const(vec.result())
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
          }
        case BodyCodecResult(_) => noop[S]
        case NoCodecResult()    => noop[S]
      }
    }

    fields
      .find(p => p.instance.hints.get(HttpPayload).isDefined)
      .map { field => processField(field) }
      .getOrElse(noop[S])

  }

  override def withHints[A](fa: Result[A], hints: Hints): Result[A] =
    fa.addHints(hints)

  override def bijection[A, B](
      f: Result[A],
      to: A => B,
      from: B => A
  ): Result[B] = f.imap(to, from)

  override def surjection[A, B](
      f: Result[A],
      tags: List[smithy4s.ShapeTag[_]],
      to: A => Either[ConstraintError, B],
      from: B => A
  ): Result[B] = f.xmap(to, from) // inherited from trait

}
