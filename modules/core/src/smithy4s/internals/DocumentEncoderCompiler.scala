/*
 *  Copyright 2021-2026 Disney Streaming
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
import alloy.Discriminated
import alloy.JsonUnknown
import smithy4s.schema._

import scala.collection.mutable.Builder

import Document._
import smithy4s.schema.Primitive._
import alloy.Untagged
import smithy4s.schema.FieldFilter

class DocumentEncoderCompiler(fieldFilter: FieldFilter)
    extends Compilation.Visitor[DocumentEncoder] { self =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ) = leaf {
    tag match {
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
      case PDocument => from(identity)
      case PFloat    => from(float => DNumber(BigDecimal(float.toDouble)))
      case PUUID     => from(uuid => DString(uuid.toString()))
      case PDouble   => from(double => DNumber(BigDecimal(double)))
      case PLong     => from(long => DNumber(BigDecimal(long)))
      case PString   => from(DString(_))
    }
  }

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ) = compile(member).map { encoderS =>
    from[C[A]](c => DArray(tag.iterator(c).map(encoderS.apply).toIndexedSeq))
  }

  override def option[A](schema: Schema[A]) =
    compile(schema).map { encoder =>
      locally {
        case Some(a) => encoder.apply(a)
        case None    => Document.DNull
      }
    }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ) = Compilation
    .zipN(
      KeyEncoderCompiler.compile(key),
      this.compile(key),
      this.compile(value)
    )
    .map {
      case (Some(keyEncoder), _, valueEncoder) =>
        from[Map[K, V]] { map =>
          val mapBuilder = Map.newBuilder[String, Document]
          map.foreach { case (k, v) =>
            val key = keyEncoder.apply(k)
            val value = valueEncoder.apply(v)
            mapBuilder.+=((key, value))
          }
          DObject(mapBuilder.result())
        }
      case (None, keyAsValueEncoder, valueEncoder) =>
        from[Map[K, V]] { map =>
          val arrayBuilder = IndexedSeq.newBuilder[Document]
          map.map { case (k, v) =>
            arrayBuilder.+=(
              DObject(
                Map(
                  "key" -> keyAsValueEncoder.apply(k),
                  "value" -> valueEncoder.apply(v)
                )
              )
            )
          }
          DArray(arrayBuilder.result())
        }
    }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag[E],
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ) = leaf {
    tag match {
      case EnumTag.IntEnum() =>
        from(e => Document.fromInt(total(e).intValue))
      case _ =>
        from(e => DString(total(e).stringValue))
    }
  }

  private def isForJsonUnknown(field: Field[_, _]): Boolean =
    field.hints.has(JsonUnknown)

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ) = {
    val discriminator =
      hints.get(DiscriminatedUnionMember).map { discriminated =>
        (discriminated.propertyName -> Document.fromString(
          discriminated.alternativeLabel
        ))
      }
    def fieldEncoder[A](
        field: Field[S, A]
    ): Compilation[
      (S, Builder[(String, Document), Map[String, Document]]) => Unit
    ] = compile(field.schema).map { encoder =>
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
    ): Compilation[
      (S, Builder[(String, Document), Map[String, Document]]) => Unit
    ] = compile(field.schema).map { encoder =>
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
    val knownFieldsC = knownFields.map(fieldEncoder(_))
    val unknownFieldsC = fieldsForUnknown.map(jsonUnknownFieldEncoder(_))
    Compilation
      .sequence(knownFieldsC ++ unknownFieldsC)
      .map { encoders =>
        new DocumentEncoder[S] {
          def apply(s: S): Document = {
            val builder = Map.newBuilder[String, Document]
            encoders.foreach(_(s, builder))
            DObject(builder.result() ++ discriminator)
          }
        }
      }
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[Alt[U, _]],
      ordinal: U => Int
  ) = {
    object precompile extends Compilation.Precompiler[DocumentEncoder] {
      override def apply[A](
          label: String,
          schema: Schema[A]
      ): Compilation[DocumentEncoder[A]] = {
        val jsonLabel =
          schema.hints.get(JsonName).map(_.value).getOrElse(label)
        hints match {
          case Untagged.hint(_) | JsonUnknown.hint(_) => compile(schema)

          case Discriminated.hint(discriminated) =>
            val unionMemberHint = DiscriminatedUnionMember(
              discriminated.value,
              jsonLabel
            )
            compile(schema.addHints(unionMemberHint))
          case _ =>
            compile(schema).map(_.mapDocument(_.nest(jsonLabel)))
        }
      }
    }
    dispatch(alternatives, ordinal, precompile)
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ) = compile(schema).map(_.contramap(bijection.from))

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ) = compile(schema).map(_.contramap(refinement.from))

  override def lazily[A](suspend: Lazy[Schema[A]]) =
    buildRecursive(suspend) { lazyCodec =>
      new DocumentEncoder[A] {
        def apply(a: A): Document = lazyCodec.value(a)
      }
    }

  def from[A](f: A => Document): DocumentEncoder[A] =
    new DocumentEncoder[A] {
      def apply(a: A): Document = f(a)
    }
}
