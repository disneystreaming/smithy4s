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

package smithy4s.internals

import smithy4s.schema._
import smithy4s.internals.PatternDecode.MaybePatternDecode
import smithy4s.Bijection
import smithy4s.{Hints, Lazy, Refinement, ShapeId, IntEnum, Timestamp}
import smithy.api.TimestampFormat

private[internals] final class SchemaVisitorPatternDecoder(
    segments: List[PatternSegment]
) extends SchemaVisitor[MaybePatternDecode]
    with SchemaVisitor.Default[MaybePatternDecode] {
  self =>

  def default[A]: MaybePatternDecode[A] = None

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): MaybePatternDecode[P] = {
    tag match {
      case Primitive.PShort      => PatternDecode.from(_.toShort)
      case Primitive.PInt        => PatternDecode.from(_.toInt)
      case Primitive.PFloat      => PatternDecode.from(_.toFloat)
      case Primitive.PLong       => PatternDecode.from(_.toLong)
      case Primitive.PDouble     => PatternDecode.from(_.toDouble)
      case Primitive.PBigInt     => PatternDecode.from(BigInt(_))
      case Primitive.PBigDecimal => PatternDecode.from(BigDecimal(_))
      case Primitive.PBoolean    => PatternDecode.from(_.toLowerCase == "true")
      case Primitive.PString     => PatternDecode.from(identity)
      case Primitive.PUUID => PatternDecode.from(java.util.UUID.fromString(_))
      case Primitive.PByte => PatternDecode.from(_.toByte)
      case Primitive.PBlob => default
      case Primitive.PDocument => default
      case Primitive.PTimestamp =>
        val fmt =
          hints.get(TimestampFormat).getOrElse(TimestampFormat.DATE_TIME)
        PatternDecode.from(
          Timestamp
            .parse(_, fmt)
            .getOrElse(throw StructurePatternError("unable to parse timestamp"))
        )
      case Primitive.PUnit => default
    }
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): MaybePatternDecode[E] = {
    val fromName = values.map(e => e.stringValue -> e.value).toMap
    if (hints.has[IntEnum]) {
      val fromOrdinal =
        values.map(e => BigDecimal(e.intValue) -> e.value).toMap
      PatternDecode.from(value =>
        if (fromOrdinal.contains(BigDecimal(value)))
          fromOrdinal(BigDecimal(value))
        else throw StructurePatternError(s"Enum case for '$value' not found.")
      )
    } else {
      PatternDecode.from(value =>
        if (fromName.contains(value)) fromName(value)
        else throw StructurePatternError(s"Enum case for '$value' not found.")
      )
    }
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): MaybePatternDecode[S] = {
    val fieldDecoders = fields.map { field =>
      field.label -> self(field.instance)
        .getOrElse(
          throw StructurePatternError(
            s"Unable to create decoder for ${field.label}"
          )
        )
    }
    PatternDecode.from { input =>
      val (fieldStrings, leftOverInput) =
        segments.foldLeft((Map.empty[String, String], input)) {
          case ((acc, remainingInput), segment) =>
            segment match {
              case PatternSegment.StaticSegment(value) =>
                val length = value.length
                val taken = remainingInput.take(length)
                if (taken != value)
                  throw StructurePatternError(
                    s"Incorrect pattern, expected '$value' but found '$taken'"
                  )
                else (acc, remainingInput.drop(length))
              case PatternSegment.ParameterSegment(
                    paramName,
                    terminationChar
                  ) =>
                val paramValue =
                  remainingInput.takeWhile(i => !terminationChar.contains(i))
                if (paramValue.isEmpty)
                  throw StructurePatternError(
                    "Empty parameter value encountered"
                  )
                (
                  acc + (paramName -> paramValue),
                  remainingInput.drop(paramValue.length)
                )
            }
        }

      if (leftOverInput.nonEmpty)
        throw StructurePatternError(
          s"Extra characters found in input string '$leftOverInput'"
        )

      val decodedFields = fieldDecoders.map { case (fieldLabel, fieldDecoder) =>
        fieldStrings.get(fieldLabel) match {
          case Some(fieldValue) => fieldDecoder.decode(fieldValue)
          case None =>
            throw StructurePatternError(s"No decoder found for '$fieldLabel'")
        }
      }

      make(decodedFields)
    }
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): MaybePatternDecode[B] = {
    self(schema).map(_.map(bijection.to))
  }

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): MaybePatternDecode[B] = {
    self(schema).map(_.map(refinement.apply(_) match {
      case Left(error)  => throw new StructurePatternError(error)
      case Right(value) => value
    }))
  }

  override def lazily[A](suspend: Lazy[Schema[A]]): MaybePatternDecode[A] = None

}
