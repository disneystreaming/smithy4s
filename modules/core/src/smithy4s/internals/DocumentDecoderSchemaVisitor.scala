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
package internals

import alloy.Discriminated
import smithy.api.JsonName
import smithy.api.TimestampFormat
import smithy.api.TimestampFormat.DATE_TIME
import smithy.api.TimestampFormat.EPOCH_SECONDS
import smithy.api.TimestampFormat.HTTP_DATE
import smithy4s.Document._
import smithy4s.capability.Covariant
import smithy4s.http.PayloadError
import smithy4s.schema.Primitive._
import smithy4s.schema._

import java.util.Base64
import java.util.UUID

trait DocumentDecoder[A] { self =>
  def apply(history: List[PayloadPath.Segment], document: Document): A
  def expected: String

  def map[B](f: A => B): DocumentDecoder[B] = new DocumentDecoder[B] {

    def apply(path: List[PayloadPath.Segment], document: Document): B = {
      f(self(path, document))
    }

    def expected: String = self.expected
  }

  def emap[B](f: A => Either[ConstraintError, B]): DocumentDecoder[B] =
    new DocumentDecoder[B] {

      def apply(path: List[PayloadPath.Segment], document: Document): B = {
        f(self(path, document)) match {
          case Right(value) => value
          case Left(value) =>
            throw PayloadError(PayloadPath(path), expected, value.message)
        }
      }

      def expected: String = self.expected
    }
}

object DocumentDecoder {

  implicit val covariantInstance: Covariant[DocumentDecoder] =
    new Covariant[DocumentDecoder] {
      def map[A, B](fa: DocumentDecoder[A])(f: A => B): DocumentDecoder[B] =
        fa.map(f)

      def emap[A, B](fa: DocumentDecoder[A])(
          f: A => Either[ConstraintError, B]
      ): DocumentDecoder[B] =
        fa.emap(f)
    }

  def instance[A](
      expectedType: String,
      expectedJsonShape: String
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
    def expected: String = expectedType
  }

}

class DocumentDecoderSchemaVisitor(
    val cache: CompilationCache[DocumentDecoder]
) extends SchemaVisitor.Cached[DocumentDecoder] {

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): DocumentDecoder[P] = tag match {
    case PDocument =>
      new DocumentDecoder[Document] {
        def apply(path: List[PayloadPath.Segment], d: Document): Document = d

        def expected: String = "Json document"
      }
    case PShort =>
      from("Short") {
        case FlexibleNumber(bd) if bd.isValidShort => bd.shortValue
      }
    case PString =>
      from("String") { case DString(value) =>
        value
      }
    case PFloat =>
      from("Float") { case FlexibleNumber(bd) =>
        bd.toFloat
      }
    case PDouble =>
      from("Double") {
        case FlexibleNumber(bd) if bd.isDecimalDouble => bd.toDouble
      }
    case PUnit =>
      DocumentDecoder.instance("Unit", "Object") { case (_, DObject(_)) =>
        ()
      }
    case PTimestamp =>
      def forFormat(format: TimestampFormat) = {
        val formatRepr = Timestamp.showFormat(format)
        DocumentDecoder.instance("Timestamp", "String") {
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
      }
      hints match {
        case TimestampFormat.hint(format) =>
          format match {
            case DATE_TIME | HTTP_DATE => forFormat(format)
            case EPOCH_SECONDS =>
              DocumentDecoder.instance("Timestamp", "Number") {
                case (_, DNumber(value)) =>
                  val epochSeconds = value.toLong
                  Timestamp(
                    epochSeconds,
                    ((value - epochSeconds) * 1000000000).toInt
                  )
              }
          }

        case _ => forFormat(DATE_TIME)

      }
    case PBlob =>
      fromUnsafe("Base64 binary blob") { case DString(string) =>
        ByteArray(Base64.getDecoder().decode(string))
      }
    case PBigInt =>
      from("BigInt") {
        case FlexibleNumber(bd) if bd.isWhole => bd.toBigInt
      }
    case PUUID =>
      fromUnsafe("UUID") { case DString(string) =>
        UUID.fromString(string)
      }
    case PInt =>
      from("Int") {
        case FlexibleNumber(bd) if bd.isValidInt => bd.intValue
      }
    case PBigDecimal =>
      from("BigDecimal") { case FlexibleNumber(bd) =>
        bd
      }
    case PBoolean =>
      from("Boolean") {
        case DBoolean(value)  => value
        case DString("true")  => true
        case DString("false") => false
      }
    case PLong =>
      from("Long") {
        case FlexibleNumber(bd) if bd.isValidLong => bd.longValue
      }
    case PByte =>
      from("Byte") {
        case FlexibleNumber(bd) if bd.isValidByte => bd.toByte
      }
  }

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): DocumentDecoder[C[A]] = {
    val fa = apply(member)
    DocumentDecoder.instance(tag.name, "Array") { case (pp, DArray(value)) =>
      tag.fromIterator(value.iterator.zipWithIndex.map {
        case (document, index) =>
          val localPath = PayloadPath.Segment(index) :: pp
          fa(localPath, document)
      })
    }
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): DocumentDecoder[Map[K, V]] = {
    val maybeKeyDecoder = DocumentKeyDecoder.trySchemaVisitor(key)
    val valueDecoder = apply(value)
    maybeKeyDecoder match {
      case Some(keyDecoder) =>
        DocumentDecoder.instance("Map", "Object") { case (pp, DObject(map)) =>
          val builder = Map.newBuilder[K, V]
          map.foreach { case (key, value) =>
            val decodedKey = keyDecoder(DString(key)).fold(
              { case DocumentKeyDecoder.DecodeError(expectedType) =>
                val path = PayloadPath.Segment.fromString(key) :: pp
                throw PayloadError(
                  PayloadPath(path.reverse),
                  expectedType,
                  "Wrong Json shape"
                )
              },
              identity
            )
            val decodedValue = valueDecoder(key :: pp, value)
            builder.+=((decodedKey, decodedValue))
          }
          builder.result()
        }
      case None =>
        val keyDecoder = apply(key)
        DocumentDecoder.instance("Map", "Array") { case (pp, DArray(value)) =>
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

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): DocumentDecoder[E] = {
    val fromName = values.map(e => e.stringValue -> e.value).toMap
    if (hints.has[IntEnum]) {
      val fromOrdinal =
        values.map(e => BigDecimal(e.intValue) -> e.value).toMap
      from(
        s"value in [${fromName.keySet.mkString(", ")}]"
      ) {
        case DNumber(value) if fromOrdinal.contains(value) => fromOrdinal(value)
      }
    } else {
      from(
        s"value in [${fromName.keySet.mkString(", ")}]"
      ) {
        case DString(value) if fromName.contains(value) => fromName(value)
      }
    }
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): DocumentDecoder[S] = {
    def jsonLabel[A](field: Field[Schema, S, A]): String =
      field.instance.hints.get(JsonName).map(_.value).getOrElse(field.label)

    def fieldDecoder[A](
        field: Field[Schema, S, A]
    ): (
        List[PayloadPath.Segment],
        Any => Unit,
        Map[String, Document]
    ) => Unit = {
      val jLabel = jsonLabel(field)
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

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): DocumentDecoder[U] = {
    def jsonLabel[A](alt: Alt[Schema, U, A]): String =
      alt.instance.hints.get(JsonName).map(_.value).getOrElse(alt.label)

    val decoders: DecoderMap[U] =
      alternatives.map { case alt @ Alt(_, instance, inject) =>
        val label = jsonLabel(alt)
        val encoder = { (pp: List[PayloadPath.Segment], doc: Document) =>
          inject(apply(instance)(label :: pp, doc))
        }
        jsonLabel(alt) -> encoder
      }.toMap

    hints match {
      case Discriminated.hint(discriminated) =>
        discriminatedUnion(discriminated, decoders)
      case _ =>
        taggedUnion(decoders)
    }
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): DocumentDecoder[B] = apply(schema).map(bijection)

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): DocumentDecoder[B] = apply(schema).emap(refinement.asFunction)

  override def lazily[A](suspend: Lazy[Schema[A]]): DocumentDecoder[A] = {
    lazy val underlying = apply(suspend.value)
    new DocumentDecoder[A] {

      def apply(history: List[PayloadPath.Segment], document: Document): A =
        underlying.apply(history, document)

      def expected: String = underlying.expected
    }
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

  object KeyValueObj {
    def unapply(doc: Document): Option[(Document, Document)] = doc match {
      case DObject(map) =>
        map.get("key").flatMap(key => map.get("value").map(key -> _))
      case _ => None
    }
  }

  def from[A](expectedType: String)(
      f: PartialFunction[Document, A]
  ): DocumentDecoder[A] =
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
      def expected: String = expectedType
    }

  def fromUnsafe[A](expectedType: String)(
      f: PartialFunction[Document, A]
  ): DocumentDecoder[A] =
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

      def expected: String = expectedType
    }
}
