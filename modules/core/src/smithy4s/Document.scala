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

import smithy4s.http.PayloadError
import smithy4s.internals.SchematicDocumentDecoder
import smithy4s.internals.SchematicDocumentEncoder

/**
  * A json-like free-form structure serving as a model for
  * the Document datatype in smithy.
  */
sealed trait Document extends Product with Serializable {

  def decode[A](implicit
      decoder: Document.Decoder[A]
  ): Either[PayloadError, A] =
    decoder.decode(this)

}

object Document {

  def encode[A](a: A)(implicit encoder: Encoder[A]): Document =
    encoder.encode(a)

  def decode[A](document: Document)(implicit
      decoder: Decoder[A]
  ): Either[PayloadError, A] =
    decoder.decode(document)

  case class DNumber(value: BigDecimal) extends Document
  case class DString(value: String) extends Document
  case class DBoolean(value: Boolean) extends Document
  case object DNull extends Document
  case class DArray(value: IndexedSeq[Document]) extends Document
  case class DObject(value: Map[String, Document]) extends Document

  def fromString(str: String): Document = DString(str)
  def fromInt(int: Int): Document = DNumber(BigDecimal(int))
  def fromLong(long: Long): Document = DNumber(BigDecimal(long))
  def fromDouble(double: Double): Document = DNumber(BigDecimal(double))
  def fromBigDecimal(bigDecimal: BigDecimal): Document = DNumber(bigDecimal)
  def fromBoolean(bool: Boolean): Document = DBoolean(bool)
  def array(values: Document*): Document = DArray(values.toIndexedSeq)
  def array(values: Iterable[Document]): Document = DArray(
    IndexedSeq.newBuilder.++=(values).result()
  )
  def obj(kv: (String, Document)*): Document = DObject(Map(kv: _*))
  def nullDoc: Document = DNull

  trait Schematic[F[_]] {
    def document: F[Document]
  }

  object Schema extends schematic.Schema[Schematic, Document] {
    def compile[F[_]](s: Schematic[F]): F[Document] = s.document
  }

  trait ClosedSyntax[S[x[_]] <: Schematic[x]] {
    val document: schematic.Schema[S, Document] = Schema
  }

  trait Encoder[A] {
    def encode(a: A): Document
  }

  object Encoder {

    def fromSchema[A](schema: smithy4s.Schema[A]): Encoder[A] = {
      val makeEncoder = schema.compile(SchematicDocumentEncoder).get
      new Encoder[A] {
        val encodeFunction = makeEncoder.apply
        def encode(a: A): Document = encodeFunction(a)
      }
    }

    implicit def deriveEncoderFromStaticSchema[A](implicit
        schema: Static[smithy4s.Schema[A]]
    ): Encoder[A] = encoderCache(schema)

    private val encoderCache =
      new schematic.PolyFunction[smithy4s.Schema, Encoder] {
        def apply[A](fa: smithy4s.Schema[A]): Encoder[A] = fromSchema(fa)
      }.unsafeMemoise

  }

  trait Decoder[A] {
    def decode(a: Document): Either[PayloadError, A]
  }

  object Decoder {

    def fromSchema[A](schema: smithy4s.Schema[A]): Decoder[A] = {
      val makeDecoder = schema.compile(SchematicDocumentDecoder)
      new Decoder[A] {
        val decodeFunction = makeDecoder.get
        def decode(a: Document): Either[PayloadError, A] =
          try { Right(decodeFunction(Nil, a)) }
          catch {
            case e: PayloadError => Left(e)
          }
      }
    }

    implicit def derivedDecoderFromStaticSchema[A](implicit
        schema: Static[smithy4s.Schema[A]]
    ): Decoder[A] = decoderCache(schema)

    private val decoderCache =
      new schematic.PolyFunction[smithy4s.Schema, Decoder] {
        def apply[A](fa: smithy4s.Schema[A]): Decoder[A] = fromSchema(fa)
      }.unsafeMemoise

  }

}
