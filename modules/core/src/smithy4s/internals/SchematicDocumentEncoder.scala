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

import smithy4s.schema._
import smithy.api.JsonName
import smithy.api.TimestampFormat
import smithy.api.TimestampFormat.DATE_TIME
import smithy.api.TimestampFormat.EPOCH_SECONDS
import smithy.api.TimestampFormat.HTTP_DATE
import smithy4s.capability.Contravariant

import java.util.Base64
import java.util.UUID
import scala.collection.mutable.Builder

import Document._
import DocumentEncoder.DocumentEncoderMake
import smithy4s.api.Discriminated

trait DocumentEncoder[A] { self =>

  def canBeKey: Boolean

  def apply: A => Document
  def contramap[B](f: B => A): DocumentEncoder[B] =
    new DocumentEncoder[B] {

      def apply: B => Document = f andThen self.apply

      def canBeKey: Boolean = self.canBeKey
    }

}

object DocumentEncoder {
  type DocumentEncoderMake[A] = Hinted[DocumentEncoder, A]

  implicit val contraInstance: Contravariant[DocumentEncoder] =
    new Contravariant[DocumentEncoder] {
      def contramap[A, B](fa: DocumentEncoder[A])(
          f: B => A
      ): DocumentEncoder[B] = fa.contramap(f)
    }
}

object SchematicDocumentEncoder extends Schematic[DocumentEncoderMake] {

  def from[A](f: A => Document): DocumentEncoderMake[A] = Hinted.static {
    new DocumentEncoder[A] {
      def apply: A => Document = f
      def canBeKey: Boolean = true
    }
  }

  def fromNotKey[A](f: A => Document): DocumentEncoderMake[A] = Hinted.static {
    new DocumentEncoder[A] {
      def apply: A => Document = f
      def canBeKey: Boolean = false
    }
  }

  def short: DocumentEncoderMake[Short] =
    from(short => DNumber(BigDecimal(short.toInt)))

  def int: DocumentEncoderMake[Int] =
    from(int => DNumber(BigDecimal(int)))

  def long: DocumentEncoderMake[Long] =
    from(long => DNumber(BigDecimal(long)))

  def double: DocumentEncoderMake[Double] =
    from(double => DNumber(BigDecimal(double)))

  def float: DocumentEncoderMake[Float] =
    from(float => DNumber(BigDecimal(float.toDouble)))

  def bigint: DocumentEncoderMake[BigInt] =
    from(bigInt => DNumber(BigDecimal(bigInt)))

  def bigdecimal: DocumentEncoderMake[BigDecimal] =
    from(DNumber(_))

  def string: DocumentEncoderMake[String] =
    from(DString(_))

  def boolean: DocumentEncoderMake[Boolean] =
    from(DBoolean(_))

  def uuid: DocumentEncoderMake[UUID] =
    from(uuid => DString(uuid.toString()))

  def byte: DocumentEncoderMake[Byte] =
    from(byte => DNumber(BigDecimal(byte.toInt)))

  def bytes: DocumentEncoderMake[ByteArray] =
    from(bytes => DString(Base64.getEncoder().encodeToString(bytes.array)))

  def unit: DocumentEncoderMake[Unit] = fromNotKey(_ => DObject(Map.empty))

  def list[S](fs: DocumentEncoderMake[S]): DocumentEncoderMake[List[S]] =
    fs.transform(encoderS =>
      fromNotKey[List[S]](l => DArray(l.map(encoderS.apply).toIndexedSeq)).get
    )

  def set[S](fs: DocumentEncoderMake[S]): DocumentEncoderMake[Set[S]] =
    fs.transform(encoderS =>
      fromNotKey[Set[S]](s => DArray(s.map(encoderS.apply).toIndexedSeq)).get
    )

  def vector[S](fs: DocumentEncoderMake[S]): DocumentEncoderMake[Vector[S]] =
    fs.transform(encoderS =>
      fromNotKey[Vector[S]](v => DArray(v.map(encoderS.apply).toIndexedSeq)).get
    )

  def map[K, V](
      fk: DocumentEncoderMake[K],
      fv: DocumentEncoderMake[V]
  ): DocumentEncoderMake[Map[K, V]] = {
    fk.productTransform(fv) { (keyEncoder, valueEncoder) =>
      fromNotKey[Map[K, V]] { map =>
        val keysAndValues: Iterator[(Document, Document)] = map.map {
          case (k, v) =>
            (keyEncoder.apply(k), valueEncoder.apply(v))
        }.iterator
        if (keyEncoder.canBeKey) {
          val mapBuilder = Map.newBuilder[String, Document]
          keysAndValues.foreach {
            case (DString(s), value)  => mapBuilder.+=((s, value))
            case (DNumber(b), value)  => mapBuilder.+=((b.toString(), value))
            case (DBoolean(b), value) => mapBuilder.+=((b.toString(), value))
            case _                    => ()
          }
          DObject(mapBuilder.result())
        } else {
          val arrayBuilder = IndexedSeq.newBuilder[Document]
          keysAndValues.foreach { case (k, v) =>
            arrayBuilder.+=(DObject(Map("key" -> k, "value" -> v)))
          }
          DArray(arrayBuilder.result())
        }
      }.get
    }
  }

  def struct[S](fields: Vector[Field[DocumentEncoderMake, S, _]])(
      const: Vector[Any] => S
  ): DocumentEncoderMake[S] =
    Hinted[DocumentEncoder].onHintOpt[DiscriminatedUnionMember, S] {
      maybeDiscriminated =>
        val discriminator = maybeDiscriminated.map { discriminated =>
          (discriminated.propertyName -> Document.fromString(
            discriminated.alternativeLabel
          ))
        }
        def fieldEncoder[A](
            field: Field[DocumentEncoderMake, S, A]
        ): (S, Builder[(String, Document), Map[String, Document]]) => Unit = {
          val encoder = field.instance
          (s, builder) =>
            field.foreachT(s) { t =>
              val jsonLabel = field.instance.hints
                .get(JsonName)
                .map(_.value)
                .getOrElse(field.label)

              builder.+=(jsonLabel -> encoder.get.apply(t))
            }
        }

        val encoders = fields.map(field => fieldEncoder(field))
        new DocumentEncoder[S] {
          def apply: S => Document = { s =>
            val builder = Map.newBuilder[String, Document]
            encoders.foreach(_(s, builder))
            DObject(builder.result() ++ discriminator)
          }
          def canBeKey: Boolean = false
        }
    }

  def union[S](
      first: Alt[DocumentEncoderMake, S, _],
      rest: Vector[Alt[DocumentEncoderMake, S, _]]
  )(
      total: S => Alt.WithValue[DocumentEncoderMake, S, _]
  ): DocumentEncoderMake[S] = {
    def jsonLabel[A](alt: Alt[DocumentEncoderMake, S, A]): String =
      alt.instance.hints.get(JsonName).map(_.value).getOrElse(alt.label)
    val handle = handleUnion(first, rest, total) _
    Hinted[DocumentEncoder].onHintOpt[Discriminated, S] {
      case Some(discriminated) =>
        handle(
          (alt: Alt[DocumentEncoderMake, S, _]) =>
            alt.instance.addHints(
              Hints(
                DiscriminatedUnionMember(
                  discriminated.value,
                  jsonLabel(alt)
                )
              )
            ),
          (alt: Alt[DocumentEncoderMake, S, _], doc: Document) => doc
        )
      case None =>
        handle(
          (alt: Alt[DocumentEncoderMake, S, _]) => alt.instance,
          (alt: Alt[DocumentEncoderMake, S, _], doc: Document) =>
            Document.obj(jsonLabel(alt) -> doc)
        )

    }
  }

  private def handleUnion[S](
      first: Alt[DocumentEncoderMake, S, _],
      rest: Vector[Alt[DocumentEncoderMake, S, _]],
      total: S => Alt.WithValue[DocumentEncoderMake, S, _]
  )(
      alterEncoder: Alt[DocumentEncoderMake, S, _] => DocumentEncoderMake[_],
      alterDocument: (Alt[DocumentEncoderMake, S, _], Document) => Document
  ): DocumentEncoder[S] = {
    val encoders: Map[Alt[DocumentEncoderMake, S, Any], Any => Document] =
      (first +: rest).map { alt =>
        val instance = alterEncoder(alt)
        val encoder = instance.get.apply
        alt.asInstanceOf[Alt[DocumentEncoderMake, S, Any]] -> encoder
          .asInstanceOf[Any => Document]
      }.toMap

    new DocumentEncoder[S] {
      def apply: S => Document = { s =>
        val awv = total(s)
        val altDoc =
          encoders(awv.alt.asInstanceOf[Alt[DocumentEncoderMake, S, Any]])(
            awv.value
          )
        alterDocument(awv.alt, altDoc)
      }
      def canBeKey: Boolean = false
    }
  }

  def enumeration[A](
      to: A => (String, Int),
      fromName: Map[String, A],
      fromOrdinal: Map[Int, A]
  ): DocumentEncoderMake[A] = from(a => DString(to(a)._1))

  def suspend[A](f: Lazy[DocumentEncoderMake[A]]): DocumentEncoderMake[A] =
    Hinted.static {
      new DocumentEncoder[A] {
        lazy val underlying = f.value.get
        def canBeKey: Boolean = underlying.canBeKey

        def apply: A => Document = underlying.apply
      }
    }

  def bijection[A, B](
      f: DocumentEncoderMake[A],
      to: A => B,
      from: B => A
  ): DocumentEncoderMake[B] = f.contramap(from)

  def timestamp: DocumentEncoderMake[Timestamp] = Hinted[DocumentEncoder].from {
    hints =>
      new DocumentEncoder[Timestamp] {
        def canBeKey: Boolean = true

        def apply: Timestamp => Document =
          hints
            .get(TimestampFormat)
            .getOrElse(TimestampFormat.DATE_TIME) match {
            case DATE_TIME     => ts => DString(ts.format(DATE_TIME))
            case HTTP_DATE     => ts => DString(ts.format(HTTP_DATE))
            case EPOCH_SECONDS => ts => DNumber(BigDecimal(ts.epochSecond))
          }
      }
  }

  def withHints[A](
      fa: DocumentEncoderMake[A],
      hints: Hints
  ): DocumentEncoderMake[A] = fa.addHints(hints)

  def document: DocumentEncoderMake[Document] = fromNotKey(identity)

}
