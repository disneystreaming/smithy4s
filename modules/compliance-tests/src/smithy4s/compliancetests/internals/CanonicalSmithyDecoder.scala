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

package smithy4s.compliancetests.internals

import smithy4s.internals._
import smithy4s.{Hints, ShapeId}
import smithy4s.schema.Primitive
import smithy4s.Document
import smithy4s.Document._
import smithy4s.schema._
import smithy4s.schema.Primitive._
import smithy4s.Timestamp
import smithy4s.ByteArray
import smithy4s.codecs.PayloadError

object CanonicalSmithyDecoder {

  /**
    * Produces a document decoder that
    *
    * @param schema
    * @return
    */
  def fromSchema[A](
      schema: Schema[A]
  ): Document.Decoder[A] = {
    decoder.fromSchema(
      schema.transformHintsTransitively(_ => Hints.empty)
    )
  }

  private object decoder extends CachedSchemaCompiler.Impl[Decoder] {

    protected type Aux[A] = smithy4s.internals.DocumentDecoder[A]

    def fromSchema[A](
        schema: Schema[A],
        cache: Cache
    ): Decoder[A] = {
      val decodeFunction =
        schema.compile(new SmithyNodeDocumentDecoderSchemaVisitor(cache))
      new Decoder[A] {
        def read(a: Document): Either[PayloadError, A] =
          try {
            Right(decodeFunction(Nil, a))
          } catch {
            case e: PayloadError => Left(e)
          }
      }
    }

  }
  private class SmithyNodeDocumentDecoderSchemaVisitor(
      override val cache: CompilationCache[DocumentDecoder]
  ) extends DocumentDecoderSchemaVisitor(cache) {
    self =>
    override def primitive[P](
        shapeId: ShapeId,
        hints: Hints,
        tag: Primitive[P]
    ): DocumentDecoder[P] = tag match {
      case PFloat  => float
      case PDouble => double
      case PTimestamp =>
        DocumentDecoder.instance("Timestamp", "Number") {
          case (_, DNumber(value)) =>
            val epochSeconds = value.toLong
            Timestamp(
              epochSeconds,
              ((value - epochSeconds) * 1000000000).toInt
            )
        }
      case PBlob =>
        from("Base64 binary blob") { case DString(string) =>
          ByteArray(string.getBytes)
        }
      case _ => super.primitive(shapeId, hints, tag)
    }

    val double = from("Double") {
      case DNumber(bd) =>
        bd.toDouble
      case DString(string) =>
        string.toDouble
    }

    val float = from("Float") {
      case DNumber(bd) =>
        bd.toFloat
      case DString(string) =>
        string.toFloat
    }

  }
}
