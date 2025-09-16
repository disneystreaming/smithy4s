/*
 *  Copyright 2021-2025 Disney Streaming
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
import alloy.JsonUnknown
import alloy.Untagged
import smithy.api.JsonName
import smithy.api.TimestampFormat
import smithy.api.TimestampFormat.DATE_TIME
import smithy.api.TimestampFormat.EPOCH_SECONDS
import smithy.api.TimestampFormat.HTTP_DATE
import smithy4s.capability.EncoderK
import smithy4s.schema.FieldFilter
import smithy4s.schema.Primitive._
import smithy4s.schema._
import smithy4s.time.DurationOps._

import scala.collection.mutable.Builder

import Document._

trait DocumentEncoder[A] { self =>

  def apply(a: A): Document
  final def contramap[B](f: B => A): DocumentEncoder[B] =
    new DocumentEncoder[B] {
      def apply(b: B): Document = self.apply(f(b))
    }

  final def mapDocument(f: Document => Document): DocumentEncoder[A] =
    new DocumentEncoder[A] {
      def apply(a: A): Document = f(self(a))
    }

}

object DocumentEncoder {

  implicit val encoderKInstance: EncoderK[DocumentEncoder] =
    new EncoderK[DocumentEncoder] {
      type Result = Document
      def apply[A](fa: DocumentEncoder[A], a: A): Document = fa.apply(a)
      def absorb[A](f: A => Document): DocumentEncoder[A] =
        new DocumentEncoder[A] {
          def apply(a: A): Document = f(a)
        }
    }

}

class DocumentEncoderSchemaVisitor(
    val cache: CompilationCache[DocumentEncoder],
    val fieldFilter: FieldFilter
) extends SchemaVisitor.Cached[DocumentEncoder] {
  self =>

  def this(
      cache: CompilationCache[DocumentEncoder],
      explicitDefaultsEncoding: Boolean
  ) =
    this(
      cache,
      fieldFilter =
        if (explicitDefaultsEncoding) FieldFilter.EncodeAll
        else FieldFilter.Default
    )

  def this(cache: CompilationCache[DocumentEncoder]) =
    this(cache, explicitDefaultsEncoding = false)

  @deprecated
  protected val explicitDefaultsEncoding: Boolean =
    fieldFilter == FieldFilter.EncodeAll

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
      from(bytes => DString(bytes.toBase64String))
    case PTimestamp =>
      hints
        .get(TimestampFormat)
        .getOrElse(TimestampFormat.EPOCH_SECONDS) match {
        case DATE_TIME => ts => DString(ts.format(DATE_TIME))
        case HTTP_DATE => ts => DString(ts.format(HTTP_DATE))
        case EPOCH_SECONDS =>
          ts =>
            DNumber(
              BigDecimal({
                val es = java.math.BigDecimal.valueOf(ts.epochSecond)
                if (ts.nano == 0) es
                else
                  es.add(
                    java.math.BigDecimal
                      .valueOf(ts.nano.toLong, 9)
                      .stripTrailingZeros
                  )
              })
            )
      }
    case PDocument  => from(identity)
    case PFloat     => from(float => DNumber(BigDecimal(float.toDouble)))
    case PUUID      => from(uuid => DString(uuid.toString()))
    case PDouble    => from(double => DNumber(BigDecimal(double)))
    case PLong      => from(long => DNumber(BigDecimal(long)))
    case PString    => from(DString(_))
    case PLocalDate => from(localDate => DString(localDate.toString()))
    case PLocalTime => from(localTime => DString(localTime.toString()))
    case PDuration  => from(duration => DNumber(duration.toBigDecimal))
    case POffsetDateTime =>
      from(offsetDateTime => DString(offsetDateTime.toString()))
  }

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): DocumentEncoder[C[A]] = {
    val encoderS = self(member)
    from[C[A]](c => DArray(tag.iterator(c).map(encoderS.apply).toIndexedSeq))
  }

  override def option[C[_], A](
      tag: OptionalTag[C],
      schema: Schema[A]
  ): DocumentEncoder[C[A]] = {
    val encoder = self(schema)
    optional => {
      tag.fold(optional, encoder.apply(_), Document.DNull)
    }
  }

  override def map[C[_, _], K, V](
      shapeId: ShapeId,
      hints: Hints,
      tag: MapTag[C],
      key: Schema[K],
      value: Schema[V]
  ): DocumentEncoder[C[K, V]] = {
    val maybeKeyEncoder = DocumentKeyEncoder.trySchemaVisitor(key)
    val valueEncoder = self(value)
    maybeKeyEncoder match {
      case Some(keyEncoder) =>
        from[C[K, V]] { c =>
          val map = tag.build[String, Document] { put =>
            tag.iterator(c).foreach { case (k, v) =>
              put(keyEncoder.apply(k), valueEncoder.apply(v))
            }
          }

          DObject(tag.toScalaMap(map))
        }
      case None =>
        from[C[K, V]] { c =>
          val keyAsValueEncoder = apply(key)

          val array = tag
            .iterator(c)
            .map { case (k, v) =>
              DObject(
                Map(
                  "key" -> keyAsValueEncoder.apply(k),
                  "value" -> valueEncoder.apply(v)
                )
              )
            }
            .toIndexedSeq
          DArray(array)
        }
    }
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag[E],
      values: List[EnumValue[E]]
  ): DocumentEncoder[E] =
    tag match {
      case EnumTag.IntEnum(value, _) =>
        from(e => Document.fromInt(value(e)))
      case EnumTag.StringEnum(value, _) =>
        from(e => DString(value(e)))
    }

  private def isForJsonUnknown(field: Field[_, _]): Boolean =
    field.hints.has(JsonUnknown)

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): DocumentEncoder[S] = {
    val discriminator =
      DiscriminatedUnionMember.hint.unapply(hints).map { discriminated =>
        (discriminated.propertyName -> Document.fromString(
          discriminated.alternativeLabel
        ))
      }
    def fieldEncoder[A](
        field: Field[S, A]
    ): (S, Builder[(String, Document), Map[String, Document]]) => Unit = {
      val encoder = apply(field.schema)
      val jsonLabel = field.hints
        .get(JsonName)
        .map(_.value)
        .getOrElse(field.label)
      val shouldRender = fieldFilter.compile(field)
      (s, builder) =>
        val value = field.get(s)
        if (shouldRender(value)) {
          builder.+=(jsonLabel -> encoder.apply(value))
        }
    }

    def jsonUnknownFieldEncoder[A](
        field: Field[S, A]
    ): (S, Builder[(String, Document), Map[String, Document]]) => Unit = {
      val encoder = apply(field.schema)
      val shouldRender = fieldFilter.compile(field)
      (s, builder) => {
        val value = field.get(s)
        if (shouldRender(value)) {
          encoder(value) match {
            case Document.DObject(value) => value.foreach(builder += _)
            case _ =>
              throw new IllegalArgumentException(
                s"Failed encoding field ${field.label} because it cannot be converted to a JSON object"
              )
          }
        }
      }
    }

    val (fieldsForUnknown, knownFields) = fields.partition(isForJsonUnknown)

    val encoders = knownFields.map(field => fieldEncoder(field)) ++
      fieldsForUnknown.map(field => jsonUnknownFieldEncoder(field))
    new DocumentEncoder[S] {
      def apply(s: S): Document = {
        val builder = Map.newBuilder[String, Document]
        encoders.foreach(_(s, builder))
        DObject(builder.result() ++ discriminator)
      }
    }
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[Alt[U, _]],
      dispatcher: Alt.Dispatcher[U]
  ): DocumentEncoder[U] = {
    val precompile = new Alt.Precompiler[DocumentEncoder] {
      def apply[A](label: String, schema: Schema[A]): DocumentEncoder[A] = {
        val jsonLabel =
          schema.hints.get(JsonName).map(_.value).getOrElse(label)
        hints match {
          // NB. just like in untagged unions
          case _ if schema.hints.has(JsonUnknown) =>
            self.apply(schema)

          case Discriminated.hint(discriminated) =>
            val unionMemberHint = DiscriminatedUnionMember(
              discriminated.value,
              jsonLabel
            )
            self.apply(schema.addHints(unionMemberHint))
          case Untagged.hint(_) => self.apply(schema)
          case _ =>
            self.apply(schema).mapDocument(_.nest(jsonLabel))
        }
      }
    }
    dispatcher.compile(precompile)
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): DocumentEncoder[B] =
    apply(schema).contramap(bijection.from)

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): DocumentEncoder[B] = apply(schema).contramap(refinement.from)

  override def lazily[A](suspend: Lazy[Schema[A]]): DocumentEncoder[A] = {
    lazy val underlying = apply(suspend.value)
    new DocumentEncoder[A] {
      def apply(a: A): Document = underlying(a)
    }
  }

  def from[A](f: A => Document): DocumentEncoder[A] =
    new DocumentEncoder[A] {
      def apply(a: A): Document = f(a)
    }
}
