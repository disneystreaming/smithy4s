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

package smithy4s.protobuf.internals

import alloy.proto.ProtoCompactUUID
import alloy.proto.ProtoIndex
import alloy.proto.ProtoInlinedOneOf
import alloy.proto.ProtoNumType
import alloy.proto.ProtoTimestampFormat
import alloy.proto.ProtoWrapped
import smithy4s.Document.DArray
import smithy4s.Document.DBoolean
import smithy4s.Document.DNull
import smithy4s.Document.DNumber
import smithy4s.Document.DObject
import smithy4s.Document.DString
import smithy4s.protobuf.internals.TaggedCodec._
import smithy4s.schema.CompilationCache
import smithy4s.schema.EnumTag.ClosedIntEnum
import smithy4s.schema.EnumTag.ClosedStringEnum
import smithy4s.schema.EnumTag.OpenIntEnum
import smithy4s.schema.EnumTag.OpenStringEnum
import smithy4s.schema.SchemaVisitor
import smithy4s.schema._
import smithy4s.{Schema => _, _}

import java.util.UUID
import smithy.api.Required
import smithy4s.protobuf.ProtobufReadError

// scalafmt: {maxColumn = 120}
private[protobuf] class TaggedCodecSchemaVisitor(val cache: CompilationCache[TaggedCodec])
    extends SchemaVisitor.Cached[TaggedCodec] {

  def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      primitiveTag: Primitive[P]
  ): TaggedCodec[P] = {
    import ScalarCodec._
    import NonScalarPrimitiveCodec._
    import TaggedCodec.ScalarFieldCodec.{apply => wrapScalar}
    import TaggedCodec.NonScalarPrimitiveFieldCodec.{apply => wrapLen}
    import Primitive._
    // See https://github.com/disneystreaming/smithy-translate#primitives-1
    val underlying: TaggedCodec[P] = primitiveTag match {
      case PLong    => wrapScalar(longCodec(hints.get(ProtoNumType)))
      case PInt     => wrapScalar(intCodec(hints.get(ProtoNumType)))
      case PBoolean => wrapScalar(BooleanCodec)
      case PDouble  => wrapScalar(DoubleCodec)
      case PFloat   => wrapScalar(FloatCodec)
      case PByte    => wrapScalar(ByteCodec)
      case PShort   => wrapScalar(ShortCodec)
      case PString  => wrapLen(StringCodec)
      case PUUID =>
        if (hints.has(ProtoCompactUUID)) {
          compactUUIDSchema.compile(this)
        } else {
          wrapLen(StringCodec).imap(UUID.fromString(_), _.toString())
        }
      case PBlob       => wrapLen(ByteArrayCodec).imap(Blob(_), _.toArray)
      case PBigDecimal => wrapLen(StringCodec).imap(bigDecimalConversion)
      case PBigInt     => wrapLen(StringCodec).imap(bigIntegerConversion)
      case PTimestamp =>
        if (hints.get(ProtoTimestampFormat).contains(ProtoTimestampFormat.EPOCH_MILLIS)) {
          protoTimestampMillisecondsSchema.compile(this)
        } else {
          protoTimestampSchema.compile(this)
        }
      case PDocument => protoJsonSchema.compile(this)
    }
    if (hints.has(ProtoWrapped)) underlying.wrap else underlying
  }

  private val bigDecimalConversion = {
    def to(string: String) = if (string.isEmpty()) BigDecimal(0) else BigDecimal(string)
    def from(bigDecimal: BigDecimal) = if (bigDecimal == BigDecimal(0)) "" else bigDecimal.toString()
    Bijection(to, from)
  }

  private val bigIntegerConversion = {
    def to(string: String) = if (string.isEmpty()) BigInt(0) else BigInt(string)
    def from(bigInt: BigInt) = if (bigInt == BigInt(0)) "" else bigInt.toString()
    Bijection(to, from)
  }

  private val compactUUIDSchema = Schema
    .tuple(
      Schema.long.addHints(ProtoIndex(1)),
      Schema.long.addHints(ProtoIndex(2))
    )
    .biject { (longTuple: (Long, Long)) => new UUID(longTuple._1, longTuple._2) }(uuid =>
      (uuid.getMostSignificantBits(), uuid.getLeastSignificantBits())
    )

  private val protoTimestampSchema = Schema
    .struct(
      Schema.long.required[Timestamp]("seconds", _.epochSecond).addHints(ProtoIndex(1)),
      Schema.int.required[Timestamp]("nanos", _.nano).addHints(ProtoIndex(2))
    )(Timestamp(_, _))

  private val protoTimestampMillisecondsSchema = Schema
    .struct(
      Schema.long.required[Timestamp]("milliseconds", _.epochMilli).addHints(ProtoIndex(1))
    )(Timestamp.fromEpochMilli)

  // see https://protobuf.dev/reference/protobuf/google.protobuf/#value
  // see https://github.com/protocolbuffers/protobuf/blob/5b32936822e64b796fa18fcff53df2305c6b7686/src/google/protobuf/struct.proto#L62
  private val protoJsonSchema: Schema[Document] = Schema.recursive {
    Schema
      .union(
        Schema.int
          .biject[DNull.type]((_: Int) => DNull)((_: DNull.type) => 0)
          .oneOf[Document]("null_value")
          .addHints(ProtoIndex(1)),
        Schema.double
          .oneOf[Document]("number_value", Document.fromDouble) { case Document.DNumber(value) =>
            value.toDouble
          }
          .addHints(ProtoIndex(2)),
        Schema.string
          .oneOf[Document]("string_value", Document.fromString) { case Document.DString(str) => str }
          .addHints(ProtoIndex(3)),
        Schema.boolean
          .oneOf[Document]("bool_value", Document.fromBoolean) { case Document.DBoolean(bool) => bool }
          .addHints(ProtoIndex(4)),
        Schema
          .map(Schema.string, protoJsonSchema)
          .oneOf[Document]("struct_value", Document.DObject(_)) { case Document.DObject(map) => map }
          .addHints(ProtoIndex(5), ProtoWrapped()),
        Schema
          .indexedSeq(protoJsonSchema)
          .oneOf[Document]("list_value", Document.DArray(_)) { case Document.DArray(seq) => seq }
          .addHints(ProtoIndex(6), ProtoWrapped())
      ) {
        case DNull       => 0
        case DNumber(_)  => 1
        case DString(_)  => 2
        case DBoolean(_) => 3
        case DObject(_)  => 4
        case DArray(_)   => 5
      }
      .withId(ShapeId("smithy4s.protobuf", "Json"))
  }

  def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      collectionTag: CollectionTag[C],
      member: Schema[A]
  ): TaggedCodec[C[A]] = {
    def drillDownImap[AA, B](taggedCodec: TaggedCodec.IMapCodec[AA, B]): TaggedCodec[C[B]] =
      taggedCodec.underlying match {
        case ScalarFieldCodec(scalarCodec) =>
          PackedRepeatedFieldCodec(scalarCodec.imap(taggedCodec.to, taggedCodec.from), collectionTag)
        case _ =>
          UnpackedRepeatedFieldCodec(taggedCodec, collectionTag)
      }

    val underlying = this(member) match {
      case ScalarFieldCodec(scalarCodec) => PackedRepeatedFieldCodec(scalarCodec, collectionTag)
      case imapCodec: IMapCodec[a, A]    => drillDownImap(imapCodec)
      case other                         => UnpackedRepeatedFieldCodec(other, collectionTag)
    }
    if (hints.has(ProtoWrapped)) underlying.wrap else underlying
  }

  def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): TaggedCodec[Map[K, V]] = {
    val underlying = this(
      Schema
        .vector(Schema.tuple(key.addHints(ProtoIndex(1)), value.addHints(ProtoIndex(2))))
        .biject(_.toMap)(_.toVector)
    )
    if (hints.has(ProtoWrapped)) underlying.wrap else underlying
  }

  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag[E],
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): TaggedCodec[E] = tag match {
    case ClosedIntEnum | ClosedStringEnum =>
      val allIndexed = values.forall(_.hints.has(ProtoIndex))
      val noneIndexed = values.forall(!_.hints.has(ProtoIndex))
      assert(
        allIndexed || noneIndexed,
        "Either all enum values should have explicit proto indexes, or none should have them"
      )
      val indexedValues = values.zipWithIndex.map { case (ev, index) =>
        val protoIndex = if (allIndexed) ev.hints.get(ProtoIndex).get.value else index
        (protoIndex, ev.value)
      }
      val default = indexedValues.find(_._1 == 0).get._2
      val rest = indexedValues.filterNot(_._1 == 0)
      TaggedCodec.ClosedEnumerationCodec(default, rest)
    case OpenIntEnum(unknown) =>
      def toInt(e: E) = total(e).intValue
      val intMap = values.map(v => (v.intValue, v.value)).toMap
      def fromInt(i: Int) = intMap.getOrElse(i, unknown(i))
      TaggedCodec.ScalarFieldCodec(ScalarCodec.IntCodec).imap(fromInt, toInt)
    case OpenStringEnum(unknown) =>
      def toString(e: E) = total(e).stringValue
      val stringMap = values.map(v => (v.stringValue, v.value)).toMap
      def fromString(s: String) = stringMap.getOrElse(s, unknown(s))
      TaggedCodec.NonScalarPrimitiveFieldCodec(NonScalarPrimitiveCodec.StringCodec).imap(fromString, toString)
  }

  def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): TaggedCodec[S] = {

    def compileField[A](field: Field[S, A], protoIndex: Int): TaggedCodec.FieldCodec[S, _] = {
      val codec = field.schema.compile(this)
      codec.oneOfTags match {
        case Some(oneOfTags) =>
          TaggedCodec.FieldCodec(FieldTags.OneOf(oneOfTags), codec, field.get)
        case _ =>
          val maybeRequiredCodec = if (field.hints.has(Required) && codec.isMessage) {
            TaggedCodec.StrictlyRequiredCodec(shapeId, field.label, protoIndex, codec)
          } else codec
          TaggedCodec.FieldCodec(FieldTags.Simple(protoIndex), maybeRequiredCodec, field.get)
      }
    }

    val allIndexed = fields.forall(_.hints.has(ProtoIndex))
    val noneIndexed = fields.forall(!_.hints.has(ProtoIndex))
    assert(
      allIndexed || noneIndexed,
      "Either all structure fields should have explicit proto indexes, or none should have them"
    )
    val fieldCodecs = fields.zipWithIndex.map { case (field, index) =>
      val protoIndex: Int = if (allIndexed) field.hints.get(ProtoIndex).get.value else index + 1
      compileField(field, protoIndex)
    }

    TaggedCodec.MessageCodec(fieldCodecs, make)
  }

  def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[Alt[U, _]],
      dispatch: Alt.Dispatcher[U]
  ): TaggedCodec[U] = {
    def compileAlt[A](alt: Alt[U, A], protoIndex: Int) = {
      TaggedCodec.OneOfAlternative(protoIndex, alt.schema.compile(this), alt.inject, alt.project)
    }
    val allIndexed = alternatives.forall(_.hints.has(ProtoIndex))
    val noneIndexed = alternatives.forall(!_.hints.has(ProtoIndex))
    assert(
      allIndexed || noneIndexed,
      "Either all union alternatives should have explicit proto indexes, or none should have them"
    )
    val altCodecs = alternatives.zipWithIndex.map { case (alt, index) =>
      val protoIndex: Int = if (allIndexed) alt.hints.get(ProtoIndex).get.value else index + 1
      compileAlt(alt, protoIndex)
    }

    val oneOfCodec = TaggedCodec.OneOfCodec(altCodecs, dispatch.ordinal)
    if (hints.has(ProtoInlinedOneOf)) oneOfCodec else oneOfCodec.wrap
  }

  def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): TaggedCodec[B] = this(schema).imap(bijection.to, bijection.from)

  def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): TaggedCodec[B] = {
    val throwingTo: A => B = refinement.asFunction andThen {
      case Left(error)  => throw ProtobufReadError.ViolatedConstraint(error.hint, error.message)
      case Right(value) => value
    }
    this(schema).imap(throwingTo, refinement.from)
  }

  def lazily[A](suspend: Lazy[Schema[A]]): TaggedCodec[A] =
    RecursiveCodec(suspend.map(this.apply))

  def option[A](schema: Schema[A]): TaggedCodec[Option[A]] = {
    val underlying = this(schema)
    OptionCodec(underlying)
  }

}
