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

import smithy.api.JsonName
import smithy.api.TimestampFormat
import smithy.api.TimestampFormat.DATE_TIME
import smithy.api.TimestampFormat.EPOCH_SECONDS
import smithy.api.TimestampFormat.HTTP_DATE
import smithy4s.api.Discriminated
import smithy4s.capability.Contravariant
import smithy4s.schema._

import java.util.Base64
import scala.collection.mutable.Builder

import Document._
import smithy4s.schema.Primitive.PShort
import smithy4s.schema.Primitive.PBigInt
import smithy4s.schema.Primitive.PBoolean
import smithy4s.schema.Primitive.PByte
import smithy4s.schema.Primitive.PBigDecimal
import smithy4s.schema.Primitive.PInt
import smithy4s.schema.Primitive.PBlob
import smithy4s.schema.Primitive.PUnit
import smithy4s.schema.Primitive.PTimestamp
import smithy4s.schema.Primitive.PDocument
import smithy4s.schema.Primitive.PFloat
import smithy4s.schema.Primitive.PUUID
import smithy4s.schema.Primitive.PDouble
import smithy4s.schema.Primitive.PLong
import smithy4s.schema.Primitive.PString

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

  implicit val contraInstance: Contravariant[DocumentEncoder] =
    new Contravariant[DocumentEncoder] {
      def contramap[A, B](fa: DocumentEncoder[A])(
          f: B => A
      ): DocumentEncoder[B] = fa.contramap(f)
    }
}

object DocumentEncoderSchemaVisitor extends SchemaVisitor[DocumentEncoder] {
  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): DocumentEncoder[P] = tag match {
    case PShort      => from(short => DNumber(BigDecimal(short.toInt)))
    case PBigInt     => from(bigInt => DNumber(BigDecimal(bigInt)))
    case PBoolean    => from(DBoolean(_))
    case PByte       => from(byte => DNumber(BigDecimal(byte.toInt)))
    case PBigDecimal => from(DNumber(_))
    case PInt        => from(int => DNumber(BigDecimal(int)))
    case PBlob =>
      from(bytes => DString(Base64.getEncoder().encodeToString(bytes.array)))
    case PUnit => fromNotKey(_ => DObject(Map.empty))
    case PTimestamp =>
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
    case PDocument => fromNotKey(identity)
    case PFloat    => from(float => DNumber(BigDecimal(float.toDouble)))
    case PUUID     => from(uuid => DString(uuid.toString()))
    case PDouble   => from(double => DNumber(BigDecimal(double)))
    case PLong     => from(long => DNumber(BigDecimal(long)))
    case PString   => from(DString(_))
  }

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): DocumentEncoder[C[A]] = {
    val encoderS = apply(member)
    fromNotKey[C[A]](c =>
      DArray(tag.iterator(c).map(encoderS.apply).toIndexedSeq)
    )
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): DocumentEncoder[Map[K, V]] = {
    val keyEncoder = apply(key)
    val valueEncoder = apply(value)
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
    }
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): DocumentEncoder[E] = from(a => DString(total(a).stringValue))

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): DocumentEncoder[S] = {
    val discriminator =
      DiscriminatedUnionMember.hint.unapply(hints).map { discriminated =>
        (discriminated.propertyName -> Document.fromString(
          discriminated.alternativeLabel
        ))
      }
    def fieldEncoder[A](
        field: Field[Schema, S, A]
    ): (S, Builder[(String, Document), Map[String, Document]]) => Unit = {
      val encoder = apply(field.instance)
      (s, builder) =>
        field.foreachT(s) { t =>
          val jsonLabel = field.instance.hints
            .get(JsonName)
            .map(_.value)
            .getOrElse(field.label)

          builder.+=(jsonLabel -> encoder.apply(t))
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

  private def handleUnion[U](
      alternatives: Vector[Alt[Schema, U, _]],
      total: U => Alt.WithValue[Schema, U, _]
  )(
      alterEncoder: Alt[Schema, U, _] => DocumentEncoder[_],
      alterDocument: (Alt[Schema, U, _], Document) => Document
  ): DocumentEncoder[U] = {
    val encoders: Map[Alt[Schema, U, Any], Any => Document] =
      alternatives.map { alt =>
        val encoder = alterEncoder(alt)
        alt.asInstanceOf[Alt[Schema, U, Any]] -> encoder.apply
          .asInstanceOf[Any => Document]
      }.toMap

    new DocumentEncoder[U] {
      def apply: U => Document = { s =>
        val awv = total(s)
        val altDoc =
          encoders(awv.alt.asInstanceOf[Alt[Schema, U, Any]])(
            awv.value
          )
        alterDocument(awv.alt, altDoc)
      }
      def canBeKey: Boolean = false
    }
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: U => Alt.SchemaAndValue[U, _]
  ): DocumentEncoder[U] = {
    def jsonLabel[A](alt: Alt[Schema, U, A]): String =
      alt.instance.hints.get(JsonName).map(_.value).getOrElse(alt.label)
    val handle = handleUnion(alternatives, dispatch) _
    Discriminated.hint.unapply(hints) match {
      case Some(discriminated) =>
        handle(
          (alt: Alt[Schema, U, _]) =>
            apply(
              alt.instance.addHints(
                Hints(
                  DiscriminatedUnionMember(
                    discriminated.value,
                    jsonLabel(alt)
                  )
                )
              )
            ),
          (alt: Alt[Schema, U, _], doc: Document) => doc
        )
      case None =>
        handle(
          (alt: Alt[Schema, U, _]) => apply(alt.instance),
          (alt: Alt[Schema, U, _], doc: Document) =>
            Document.obj(jsonLabel(alt) -> doc)
        )
    }
  }

  override def biject[A, B](
      schema: Schema[A],
      to: A => B,
      from: B => A
  ): DocumentEncoder[B] =
    apply(schema).contramap(from)

  override def surject[A, B](
      schema: Schema[A],
      to: Refinement[A, B],
      from: B => A
  ): DocumentEncoder[B] = apply(schema).contramap(from)

  override def lazily[A](suspend: Lazy[Schema[A]]): DocumentEncoder[A] = {
    lazy val underlying = apply(suspend.value)
    new DocumentEncoder[A] {
      def canBeKey: Boolean = underlying.canBeKey

      def apply: A => Document = underlying.apply
    }
  }

  def from[A](f: A => Document): DocumentEncoder[A] =
    new DocumentEncoder[A] {
      def apply: A => Document = f
      def canBeKey: Boolean = true
    }

  def fromNotKey[A](f: A => Document): DocumentEncoder[A] =
    new DocumentEncoder[A] {
      def apply: A => Document = f
      def canBeKey: Boolean = false
    }

}
