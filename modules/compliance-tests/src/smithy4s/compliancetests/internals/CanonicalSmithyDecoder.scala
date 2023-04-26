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

import smithy4s.IntEnum
import smithy4s.internals._
import smithy4s.{Hints, ShapeId}
import smithy4s.schema.Primitive
import smithy4s.Document
import smithy4s.Document._
import smithy4s.schema._
import smithy4s.schema.Primitive._
import smithy4s.Timestamp
import smithy4s.ByteArray
import smithy4s.http.PayloadError
import smithy4s.PayloadPath
import alloy.Discriminated

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
    Document.Decoder.fromSchema(
      schema.transformHintsTransitively(_.filter(_.keyId == IntEnum.id))
    )
  }

  private class SmithyNodeDocumentDecoderSchemaVisitor()
    extends DocumentDecoderSchemaVisitor(CompilationCache.nop[Aux]) {
    self =>
    override def primitive[P](
                               shapeId: ShapeId,
                               hints: Hints,
                               tag: Primitive[P]
                             ): DocumentDecoder[P] = tag match {
      case PFloat => float
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

    override def struct[S](
                            shapeId: ShapeId,
                            hints: Hints,
                            fields: Vector[SchemaField[S, _]],
                            make: IndexedSeq[Any] => S
                          ): DocumentDecoder[S] = {
      def fieldDecoder[A](
                           field: Field[Schema, S, A]
                         ): (
        List[PayloadPath.Segment],
          Any => Unit,
          Map[String, Document]
        ) => Unit = {
        val jLabel = field.label
        val maybeDefault = field.instance.getDefault

        if (field.isOptional) {
          (
            pp: List[PayloadPath.Segment],
            buffer: Any => Unit,
            fields: Map[String, Document]
          ) =>
            val path = PayloadPath.Segment(jLabel) :: pp
            fields
              .get(jLabel) match {
              case Some(DNull) => buffer(None)
              case Some(document) =>
                buffer(Some(apply(field.instance)(path, document)))
              case None => buffer(None)
            }
        } else {
          (
            pp: List[PayloadPath.Segment],
            buffer: Any => Unit,
            fields: Map[String, Document]
          ) =>
            val path = PayloadPath.Segment(jLabel) :: pp
            fields
              .get(jLabel) match {
              case Some(document) =>
                buffer(apply(field.instance)(path, document))
              case None if maybeDefault.isDefined =>
                buffer(apply(field.instance)(path, maybeDefault.get))
              case None =>
                throw new PayloadError(
                  PayloadPath(path.reverse),
                  "",
                  "Required field not found"
                )
            }
        }
      }

      val fieldDecoders = fields.map(field => fieldDecoder(field))

      DocumentDecoder.instance("Structure", "Object") {
        case (pp, DObject(value)) =>
          val buffer = Vector.newBuilder[Any]
          fieldDecoders.foreach(fd => fd(pp, buffer.+=(_), value))
          make(buffer.result())
      }
    }

    override def union[U](
                           shapeId: ShapeId,
                           hints: Hints,
                           alternatives: Vector[SchemaAlt[U, _]],
                           dispatch: Alt.Dispatcher[Schema, U]
                         ): DocumentDecoder[U] = {

      val decoders: DecoderMap[U] =
        alternatives.map { case Alt(label, instance, inject) =>
          val encoder = { (pp: List[PayloadPath.Segment], doc: Document) =>
            inject(apply(instance)(label :: pp, doc))
          }
          label -> encoder
        }.toMap

      hints match {
        case Discriminated.hint(discriminated) =>
          discriminatedUnion(discriminated, decoders)
        case _ =>
          taggedUnion(decoders)
      }
    }

    private def handleUnion[S](
                                f: (List[PayloadPath.Segment], Document) => S
                              ): DocumentDecoder[S] =
      new DocumentDecoder[S] {
        def expected: String = "Union"

        def apply(pp: List[PayloadPath.Segment], document: Document): S =
          f(pp, document)
      }

    private type DecoderMap[S] =
      Map[String, (List[PayloadPath.Segment], Document) => S]

    private def discriminatedUnion[S](
                                       discriminated: Discriminated,
                                       decoders: DecoderMap[S]
                                     ): DocumentDecoder[S] = handleUnion {
      (pp: List[PayloadPath.Segment], document: Document) =>
        document match {
          case DObject(map) =>
            map
              .get(discriminated.value) match {
              case Some(value: Document.DString) =>
                decoders.get(value.value) match {
                  case Some(decoder) => decoder(pp, document)
                  case None =>
                    throw new PayloadError(
                      PayloadPath(pp.reverse),
                      "Union",
                      s"Unknown discriminator: ${value.value}"
                    )
                }
              case _ =>
                throw new PayloadError(
                  PayloadPath(pp.reverse),
                  "Union",
                  s"Unable to locate discriminator under property '${discriminated.value}'"
                )
            }
          case other =>
            throw new PayloadError(
              PayloadPath(pp.reverse),
              "Union",
              s"Expected DObject, but found $other"
            )

        }
    }

    private def taggedUnion[S](
                                decoders: DecoderMap[S]
                              ): DocumentDecoder[S] = handleUnion {
      (pp: List[PayloadPath.Segment], document: Document) =>
        document match {
          case DObject(map) if (map.size == 1) =>
            val (key: String, value: Document) = map.head
            decoders.get(key) match {
              case Some(decoder) => decoder(pp, value)
              case None =>
                throw new PayloadError(
                  PayloadPath(pp.reverse),
                  "Union",
                  s"Unknown discriminator: $key"
                )
            }
          case _ =>
            throw new PayloadError(
              PayloadPath(pp.reverse),
              "Union",
              "Expected a single-key Json object"
            )
        }

    }

  }
}
