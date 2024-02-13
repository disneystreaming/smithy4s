/*
 *  Copyright 2021-2024 Disney Streaming
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

import smithy4s.Document._
import smithy4s.codecs.PayloadError
import smithy4s.schema.CachedSchemaCompiler

import internals.DocumentDecoderSchemaVisitor
import internals.DocumentEncoderSchemaVisitor

/**
  * A json-like free-form structure serving as a model for
  * the Document datatype in smithy.
  */
sealed trait Document extends Product with Serializable {

  def decode[A](implicit
      decoder: Document.Decoder[A]
  ): Either[PayloadError, A] =
    decoder.decode(this)

  override def toString(): String = this.show

  def name: String = this match {
    case DNumber(_)  => "Number"
    case DString(_)  => "String"
    case DBoolean(_) => "Boolean"
    case DNull       => "Null"
    case DArray(_)   => "Array"
    case DObject(_)  => "Object"
  }

  /**
    * Toy renderer that does not comply the json specification :
    * strings aren't escaped and keys aren't quoted.
    * Do not use for any real purpose other than debugging.
    */
  def show: String = this match {
    case DNumber(value) => {
      if (value.isValidLong) value.toLong.toString()
      else value.toString()
    }
    case DBoolean(value) => value.toString
    case DString(value)  => s""""$value""""
    case DNull           => "null"
    case DArray(value)   => value.map(_.show).mkString("[", ", ", "]")
    case DObject(value) =>
      value.map { case (k, v) => k + "=" + v.show }.mkString("{", ", ", "}")
  }

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

  final val EmptyObject = DObject(Map.empty)

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
  def obj(kv: (String, Document)*): Document =
    if (kv.isEmpty) EmptyObject else DObject(Map(kv: _*))
  def nullDoc: Document = DNull

  trait Encoder[A] {
    def encode(a: A): Document
  }

  trait EncoderCompiler extends CachedSchemaCompiler[Encoder] {
    def withExplicitDefaultsEncoding(
        explicitDefaultsEncoding: Boolean
    ): EncoderCompiler
  }

  object Encoder
      extends CachedEncoderCompilerImpl(explicitDefaultsEncoding = false)

  private[smithy4s] class CachedEncoderCompilerImpl(
      explicitDefaultsEncoding: Boolean
  ) extends CachedSchemaCompiler.DerivingImpl[Encoder]
      with EncoderCompiler {

    protected type Aux[A] = internals.DocumentEncoder[A]

    def fromSchemaAux[A](
        schema: Schema[A],
        cache: AuxCache
    ): Encoder[A] = {
      val makeEncoder =
        schema.compile(
          new DocumentEncoderSchemaVisitor(cache, explicitDefaultsEncoding)
        )
      new Encoder[A] {
        def encode(a: A): Document = {
          makeEncoder.apply(a)
        }
      }
    }

    def withExplicitDefaultsEncoding(
        explicitDefaultsEncoding: Boolean
    ): EncoderCompiler = new CachedEncoderCompilerImpl(
      explicitDefaultsEncoding = explicitDefaultsEncoding
    )
  }

  type Decoder[A] =
    smithy4s.codecs.Decoder[Either[PayloadError, *], Document, A]

  implicit def decoderFromSchema[A: Schema]: Decoder[A] =
    Decoder.derivedImplicitInstance

  object Decoder extends CachedSchemaCompiler.DerivingImpl[Decoder] {

    protected type Aux[A] = internals.DocumentDecoder[A]

    def fromSchemaAux[A](
        schema: Schema[A],
        cache: AuxCache
    ): Decoder[A] = {
      val decodeFunction =
        schema.compile(new DocumentDecoderSchemaVisitor(cache))
      new Decoder[A] {
        def decode(a: Document): Either[PayloadError, A] =
          try { Right(decodeFunction(Nil, a)) }
          catch {
            case e: PayloadError => Left(e)
          }
      }
    }

  }

}
