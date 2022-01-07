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
package internals

import schematic.Alt
import schematic.ByteArray
import schematic.Field
import smithy.api.JsonName
import smithy.api.TimestampFormat
import smithy.api.TimestampFormat.DATE_TIME
import smithy.api.TimestampFormat.EPOCH_SECONDS
import smithy.api.TimestampFormat.HTTP_DATE
import smithy4s.capability.Covariant
import smithy4s.http.PayloadError

import java.util.Base64
import java.util.UUID

import Document._
import DocumentDecoder.DocumentDecoderMake
import smithy4s.api.Discriminated

trait DocumentDecoder[A] { self =>

  def canBeKey: Boolean

  def apply(history: List[PayloadPath.Segment], document: Document): A
  def expected: String

  def map[B](f: A => B): DocumentDecoder[B] = new DocumentDecoder[B] {

    def apply(path: List[PayloadPath.Segment], document: Document): B = {
      f(self(path, document))
    }

    def expected: String = self.expected
    def canBeKey: Boolean = self.canBeKey

  }

}

object DocumentDecoder {
  type DocumentDecoderMake[A] = Hinted[DocumentDecoder, A]

  implicit val covariantInstance: Covariant[DocumentDecoder] =
    new Covariant[DocumentDecoder] {
      def map[A, B](fa: DocumentDecoder[A])(f: A => B): DocumentDecoder[B] =
        fa.map(f)
    }

  def instance[A](
      expectedType: String,
      expectedJsonShape: String,
      key: Boolean
  )(
      f: PartialFunction[(List[PayloadPath.Segment], Document), A]
  ): DocumentDecoder[A] = new DocumentDecoder[A] {
    def apply(history: List[PayloadPath.Segment], document: Document): A = {
      val tuple = (history, document)
      if (f.isDefinedAt(tuple)) f(tuple)
      else
        throw PayloadError(
          PayloadPath(history.reverse),
          expectedType,
          s"Expected Json $expectedJsonShape"
        )
    }
    def canBeKey: Boolean = key
    def expected: String = expectedType
  }

}

object SchematicDocumentDecoder
    extends smithy4s.Schematic[DocumentDecoderMake]
    with schematic.struct.GenericAritySchematic[DocumentDecoderMake] {

  object FlexibleNumber {
    def unapply(doc: Document): Option[BigDecimal] = doc match {
      case DNumber(value) => Some(value)
      case DString(value) =>
        try { Some(BigDecimal(value)) }
        catch { case _: Throwable => None }
      case _ => None
    }
  }

  object KeyValueObj {
    def unapply(doc: Document): Option[(Document, Document)] = doc match {
      case DObject(map) =>
        map.get("key").flatMap(key => map.get("value").map(key -> _))
      case _ => None
    }
  }

  def from[A](expectedType: String)(
      f: PartialFunction[Document, A]
  ): DocumentDecoderMake[A] = Hinted.static {
    new DocumentDecoder[A] {
      def apply(path: List[PayloadPath.Segment], a: Document): A = {
        if (f.isDefinedAt(a)) f(a)
        else
          throw PayloadError(
            PayloadPath(path.reverse),
            expectedType,
            "Wrong Json shape"
          )
      }
      def canBeKey: Boolean = true
      def expected: String = expectedType
    }
  }

  def fromUnsafe[A](expectedType: String)(
      f: PartialFunction[Document, A]
  ): DocumentDecoderMake[A] = Hinted.static {
    new DocumentDecoder[A] {
      def apply(path: List[PayloadPath.Segment], a: Document): A = {
        if (f.isDefinedAt(a)) {
          try {
            f(a)
          } catch {
            case e: Throwable =>
              throw PayloadError(
                PayloadPath(path.reverse),
                expectedType,
                e.getMessage()
              )
          }
        } else
          throw PayloadError(
            PayloadPath(path.reverse),
            expectedType,
            "Wrong Json shape"
          )
      }

      def canBeKey: Boolean = true
      def expected: String = expectedType
    }
  }

  def short: DocumentDecoderMake[Short] = from("Short") {
    case FlexibleNumber(bd) if bd.isValidShort => bd.shortValue
  }

  def int: DocumentDecoderMake[Int] = from("Int") {
    case FlexibleNumber(bd) if bd.isValidInt => bd.intValue
  }

  def long: DocumentDecoderMake[Long] = from("Long") {
    case FlexibleNumber(bd) if bd.isValidLong => bd.longValue
  }

  def double: DocumentDecoderMake[Double] = from("Double") {
    case FlexibleNumber(bd) if bd.isDecimalDouble => bd.toDouble
  }

  def float: DocumentDecoderMake[Float] = from("Float") {
    case FlexibleNumber(bd) => bd.toFloat
  }

  def bigint: DocumentDecoderMake[BigInt] = from("BigInt") {
    case FlexibleNumber(bd) if bd.isWhole => bd.toBigInt
  }

  def bigdecimal: DocumentDecoderMake[BigDecimal] = from("BigDecimal") {
    case FlexibleNumber(bd) => bd
  }

  def string: DocumentDecoderMake[String] = from("String") {
    case DString(value) => value
  }

  def boolean: DocumentDecoderMake[Boolean] = from("Boolean") {
    case DBoolean(value)  => value
    case DString("true")  => true
    case DString("false") => false
  }

  def uuid: DocumentDecoderMake[UUID] = fromUnsafe("UUID") {
    case DString(string) => UUID.fromString(string)
  }

  def byte: DocumentDecoderMake[Byte] = from("Byte") {
    case FlexibleNumber(bd) if bd.isValidByte => bd.toByte
  }

  def bytes: DocumentDecoderMake[ByteArray] = fromUnsafe("Base64 binary blob") {
    case DString(string) => ByteArray(Base64.getDecoder().decode(string))
  }

  def unit: DocumentDecoderMake[Unit] =
    Hinted.static(DocumentDecoder.instance("Unit", "Object", false) {
      case (_, DObject(_)) =>
        ()
    })

  def list[S](fs: DocumentDecoderMake[S]): DocumentDecoderMake[List[S]] =
    fs.transform { fa =>
      DocumentDecoder.instance("List", "Array", false) {
        case (pp, DArray(value)) =>
          value.zipWithIndex.map { case (document, index) =>
            val localPath = PayloadPath.Segment(index) :: pp
            fa(localPath, document)
          }.toList
      }
    }

  def set[S](fs: DocumentDecoderMake[S]): DocumentDecoderMake[Set[S]] =
    fs.transform { fa =>
      DocumentDecoder.instance("Set", "Array", false) {
        case (pp, DArray(value)) =>
          value.zipWithIndex.map { case (document, index) =>
            val localPath = PayloadPath.Segment(index) :: pp
            fa(localPath, document)
          }.toSet
      }
    }

  def vector[S](fs: DocumentDecoderMake[S]): DocumentDecoderMake[Vector[S]] =
    fs.transform { fa =>
      DocumentDecoder.instance("Vector", "Array", false) {
        case (pp, DArray(value)) =>
          value.zipWithIndex.map { case (document, index) =>
            val localPath = PayloadPath.Segment(index) :: pp
            fa(localPath, document)
          }.toVector
      }
    }

  def map[K, V](
      fk: DocumentDecoderMake[K],
      fv: DocumentDecoderMake[V]
  ): DocumentDecoderMake[Map[K, V]] = fk.productTransform(fv) {
    (keyDecoder, valueDecoder) =>
      if (keyDecoder.canBeKey) {
        DocumentDecoder.instance("Map", "Object", false) {
          case (pp, DObject(map)) =>
            val builder = Map.newBuilder[K, V]
            map.foreach { case (key, value) =>
              val decodedKey = keyDecoder(key :: pp, DString(key))
              val decodedValue = valueDecoder(key :: pp, value)
              builder.+=((decodedKey, decodedValue))
            }
            builder.result()
        }
      } else {
        DocumentDecoder.instance("Map", "Array", false) {
          case (pp, DArray(value)) =>
            val builder = Map.newBuilder[K, V]
            var i = 0
            val newPP = PayloadPath.Segment(i) :: pp
            value.foreach {
              case KeyValueObj(k, v) =>
                val decodedKey =
                  keyDecoder(PayloadPath.Segment("key") :: newPP, k)
                val decodedValue =
                  valueDecoder(PayloadPath.Segment("value") :: newPP, v)
                builder.+=((decodedKey, decodedValue))
                i += 1
              case _ =>
                throw new PayloadError(
                  PayloadPath((PayloadPath.Segment(i) :: pp).reverse),
                  "Key Value object",
                  """Expected a Json object containing two values indexed with "key" and "value". """
                )
            }
            builder.result()
        }
      }
  }

  def genericStruct[S](fields: Vector[Field[DocumentDecoderMake, S, _]])(
      const: Vector[Any] => S
  ): DocumentDecoderMake[S] = Hinted.static {
    def jsonLabel[A](field: Field[DocumentDecoderMake, S, A]): String =
      field.instance.hints.get(JsonName).map(_.value).getOrElse(field.label)

    def fieldDecoder[A](
        field: Field[DocumentDecoderMake, S, A]
    ): (
        List[PayloadPath.Segment],
        Any => Unit,
        Map[String, Document]
    ) => Unit = {
      val jLabel = jsonLabel(field)

      if (field.isOptional) {
        (
            pp: List[PayloadPath.Segment],
            buffer: Any => Unit,
            fields: Map[String, Document]
        ) =>
          val path = PayloadPath.Segment(jLabel) :: pp
          fields
            .get(jLabel) match {
            case Some(document) =>
              buffer(Some(field.instance.get.apply(path, document)))
            case None => buffer(None)
          }
      } else {
        (
            pp: List[PayloadPath.Segment],
            buffer: Any => Unit,
            fields: Map[String, Document]
        ) =>
          val path = PayloadPath.Segment(jLabel) :: pp
          val document = fields
            .get(jLabel)
            .getOrElse(
              throw new PayloadError(
                PayloadPath(path.reverse),
                "",
                "Required field not found"
              )
            )
          buffer(field.instance.get.apply(path, document))
      }
    }

    val fieldDecoders = fields.map(field => fieldDecoder(field))

    DocumentDecoder.instance("Structure", "Object", false) {
      case (pp, DObject(value)) =>
        val buffer = Vector.newBuilder[Any]
        fieldDecoders.foreach(fd => fd(pp, buffer.+=(_), value))
        const(buffer.result())
    }
  }

  private def handleUnion[S](
      f: (List[PayloadPath.Segment], Document) => S
  ): DocumentDecoder[S] =
    new DocumentDecoder[S] {
      def expected: String = "Union"
      def apply(pp: List[PayloadPath.Segment], document: Document): S =
        f(pp, document)
      def canBeKey: Boolean = false
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
            .get(discriminated.propertyName) match {
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
                s"Unable to locate discriminator under property '${discriminated.propertyName}'"
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

  def union[S](
      first: Alt[DocumentDecoderMake, S, _],
      rest: Vector[Alt[DocumentDecoderMake, S, _]]
  )(
      total: S => Alt.WithValue[DocumentDecoderMake, S, _]
  ): DocumentDecoderMake[S] = {
    def jsonLabel[A](alt: Alt[DocumentDecoderMake, S, A]): String =
      alt.instance.hints.get(JsonName).map(_.value).getOrElse(alt.label)

    val decoders: DecoderMap[S] =
      (first +: rest).map { case alt @ Alt(_, instance, inject) =>
        val label = jsonLabel(alt)
        val encoder = { (pp: List[PayloadPath.Segment], doc: Document) =>
          inject(instance.get.apply(label :: pp, doc))
        }
        jsonLabel(alt) -> encoder
      }.toMap

    Hinted[DocumentDecoder].onHintOpt[Discriminated, S] {
      case Some(discriminated) =>
        discriminatedUnion(discriminated, decoders)
      case None =>
        taggedUnion(decoders)
    }
  }

  def enumeration[A](
      to: A => (String, Int),
      fromName: Map[String, A],
      fromOrdinal: Map[Int, A]
  ): DocumentDecoderMake[A] = from(
    s"value in [${fromName.keySet.mkString(", ")}]"
  ) {
    case DString(value) if fromName.contains(value) => fromName(value)
  }

  def suspend[A](f: => DocumentDecoderMake[A]): DocumentDecoderMake[A] =
    Hinted.static {
      new DocumentDecoder[A] {
        lazy val underlying = f.get
        def canBeKey: Boolean = underlying.canBeKey

        def apply(history: List[PayloadPath.Segment], document: Document): A =
          underlying.apply(history, document)

        def expected: String = underlying.expected

      }
    }

  def bijection[A, B](
      f: DocumentDecoderMake[A],
      to: A => B,
      from: B => A
  ): DocumentDecoderMake[B] = f.map(to)

  def timestamp: DocumentDecoderMake[Timestamp] =
    Hinted[DocumentDecoder].onHint(DATE_TIME: TimestampFormat) {
      case format @ (DATE_TIME | HTTP_DATE) =>
        val formatRepr = Timestamp.showFormat(format)
        DocumentDecoder.instance("Timestamp", "String", false) {
          case (pp, DString(value)) =>
            Timestamp
              .parse(value, format)
              .getOrElse(
                throw new PayloadError(
                  PayloadPath(pp.reverse),
                  formatRepr,
                  s"Wrong timestamp format"
                )
              )
        }
      case EPOCH_SECONDS =>
        DocumentDecoder.instance("Timestamp", "Number", false) {
          case (_, DNumber(value)) if value.isValidLong && value > 0 =>
            Timestamp.fromEpochSecond(value.toLongExact)
        }
    }

  def withHints[A](
      fa: DocumentDecoderMake[A],
      hints: Hints
  ): DocumentDecoderMake[A] = fa.addHints(hints)

  def document: DocumentDecoderMake[Document] =
    Hinted.static {
      new DocumentDecoder[Document] {
        def apply(path: List[PayloadPath.Segment], d: Document): Document = d

        def expected: String = "Json document"
        def canBeKey: Boolean = false
      }
    }

}
