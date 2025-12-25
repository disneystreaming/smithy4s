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
package json
package internals

import alloy.Discriminated
import alloy.JsonUnknown
import alloy.Nullable
import alloy.PreserveKeyOrder
import alloy.Untagged
import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader
import com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter
import smithy.api.JsonName
import smithy.api.Required
import smithy.api.TimestampFormat
import smithy4s.internals.DiscriminatedUnionMember
import smithy4s.schema.FieldFilter
import smithy4s.schema.Primitive._
import smithy4s.schema._
import smithy4s.time.DurationOps._
import smithy4s.time._

import java.util
import java.util.UUID
import scala.collection.compat.immutable.ArraySeq
import scala.collection.immutable.ListMap
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{Map => MMap}
import scala.concurrent.duration._

private[smithy4s] class SchemaVisitorJCodec(
    maxArity: Int,
    infinitySupport: Boolean,
    flexibleCollectionsSupport: Boolean,
    lenientTaggedUnionDecoding: Boolean,
    lenientNumericDecoding: Boolean,
    val cache: CompilationCache[JCodec],
    fieldFilter: FieldFilter
) extends SchemaVisitor.Cached[JCodec] { self =>
  private val emptyMetadata: MMap[String, Any] = MMap.empty

  private val allowJsonStringNumerics =
    lenientNumericDecoding || infinitySupport

  object PrimitiveJCodecs {
    val boolean: JCodec[Boolean] =
      new JCodec[Boolean] {
        def expecting: String = "boolean"

        def decodeValue(cursor: Cursor, in: JsonReader): Boolean =
          in.readBoolean()

        def encodeValue(x: Boolean, out: JsonWriter): Unit = out.writeVal(x)

        def decodeKey(in: JsonReader): Boolean = in.readKeyAsBoolean()

        def encodeKey(x: Boolean, out: JsonWriter): Unit = out.writeKey(x)
      }

    val string: JCodec[String] =
      new JCodec[String] {
        def expecting: String = "string"

        def decodeValue(cursor: Cursor, in: JsonReader): String =
          in.readString(null)

        def encodeValue(x: String, out: JsonWriter): Unit = out.writeVal(x)

        def decodeKey(in: JsonReader): String = in.readKeyAsString()

        def encodeKey(x: String, out: JsonWriter): Unit = out.writeKey(x)
      }

    private abstract class NumericJCodec[A] extends JCodec[A] {
      def decodeJsonNumber(cursor: Cursor, in: JsonReader): A
      // Allows numerics to be received as JSON Strings
      def decodeJsonString(cursor: Cursor, in: JsonReader): A

      final def decodeValue(cursor: Cursor, in: JsonReader): A =
        if (allowJsonStringNumerics) {
          if (in.isNextToken('"')) {
            in.rollbackToken()
            decodeJsonString(cursor, in)
          } else {
            in.rollbackToken()
            decodeJsonNumber(cursor, in)
          }
        } else {
          decodeJsonNumber(cursor, in)
        }
    }

    val int: JCodec[Int] = new NumericJCodec[Int] {
      def expecting: String = "int"

      def decodeJsonNumber(cursor: Cursor, in: JsonReader): Int =
        in.readInt()

      def decodeJsonString(cursor: Cursor, in: JsonReader): Int =
        in.readStringAsInt()

      def encodeValue(x: Int, out: JsonWriter): Unit = out.writeVal(x)

      def decodeKey(in: JsonReader): Int = in.readKeyAsInt()

      def encodeKey(x: Int, out: JsonWriter): Unit = out.writeKey(x)
    }

    val long: JCodec[Long] = new NumericJCodec[Long] {
      def expecting: String = "long"

      def decodeJsonNumber(cursor: Cursor, in: JsonReader): Long =
        in.readLong()

      def decodeJsonString(cursor: Cursor, in: JsonReader): Long =
        in.readStringAsLong()

      def encodeValue(x: Long, out: JsonWriter): Unit = out.writeVal(x)

      def decodeKey(in: JsonReader): Long = in.readKeyAsLong()

      def encodeKey(x: Long, out: JsonWriter): Unit = out.writeKey(x)
    }

    private val efficientFloat: JCodec[Float] = new NumericJCodec[Float] {
      def expecting: String = "float"

      def decodeJsonNumber(cursor: Cursor, in: JsonReader): Float =
        in.readFloat()

      def decodeJsonString(cursor: Cursor, in: JsonReader): Float =
        in.readStringAsFloat()

      def encodeValue(x: Float, out: JsonWriter): Unit = out.writeVal(x)

      def decodeKey(in: JsonReader): Float = in.readKeyAsFloat()

      def encodeKey(x: Float, out: JsonWriter): Unit = out.writeKey(x)
    }

    private val infinityAllowingFloat: JCodec[Float] =
      new NumericJCodec[Float] {
        val expecting: String = "JSON number for numeric values"

        def decodeJsonString(cursor: Cursor, in: JsonReader): Float = {
          in.setMark()
          val len = in.readStringAsCharBuf()
          if (in.isCharBufEqualsTo(len, "NaN")) Float.NaN
          else if (in.isCharBufEqualsTo(len, "Infinity")) Float.PositiveInfinity
          else if (in.isCharBufEqualsTo(len, "-Infinity"))
            Float.NegativeInfinity
          else {
            in.rollbackToMark()
            in.readStringAsFloat()
          }
        }

        def decodeJsonNumber(cursor: Cursor, in: JsonReader): Float = {
          in.readFloat()
        }

        def encodeValue(f: Float, out: JsonWriter): Unit =
          if (java.lang.Float.isFinite(f)) out.writeVal(f)
          else
            out.writeNonEscapedAsciiVal {
              if (f != f) "NaN"
              else if (f >= 0) "Infinity"
              else "-Infinity"
            }

        def decodeKey(in: JsonReader): Float = ???

        def encodeKey(x: Float, out: JsonWriter): Unit = ???
      }

    val float: JCodec[Float] =
      if (infinitySupport) infinityAllowingFloat else efficientFloat

    private val efficientDouble: JCodec[Double] = new NumericJCodec[Double] {
      def expecting: String = "double"

      def decodeJsonNumber(cursor: Cursor, in: JsonReader): Double =
        in.readDouble()

      def decodeJsonString(cursor: Cursor, in: JsonReader): Double =
        in.readStringAsDouble()

      def encodeValue(x: Double, out: JsonWriter): Unit = out.writeVal(x)

      def decodeKey(in: JsonReader): Double = in.readKeyAsDouble()

      def encodeKey(x: Double, out: JsonWriter): Unit = out.writeKey(x)
    }

    private val infinityAllowingDouble: JCodec[Double] =
      new NumericJCodec[Double] {
        val expecting: String = "JSON number for numeric values"

        def decodeJsonString(cursor: Cursor, in: JsonReader): Double = {
          in.setMark()
          val len = in.readStringAsCharBuf()
          if (in.isCharBufEqualsTo(len, "NaN")) Double.NaN
          else if (in.isCharBufEqualsTo(len, "Infinity"))
            Double.PositiveInfinity
          else if (in.isCharBufEqualsTo(len, "-Infinity"))
            Double.NegativeInfinity
          else {
            in.rollbackToMark()
            in.readStringAsDouble()
          }
        }

        def decodeJsonNumber(cursor: Cursor, in: JsonReader): Double =
          in.readDouble()

        def encodeValue(d: Double, out: JsonWriter): Unit =
          if (java.lang.Double.isFinite(d)) out.writeVal(d)
          else
            out.writeNonEscapedAsciiVal {
              if (d != d) "NaN"
              else if (d >= 0) "Infinity"
              else "-Infinity"
            }

        def decodeKey(in: JsonReader): Double = ???

        def encodeKey(x: Double, out: JsonWriter): Unit = ???
      }

    val double: JCodec[Double] =
      if (infinitySupport) infinityAllowingDouble else efficientDouble

    val short: JCodec[Short] = new NumericJCodec[Short] {
      def expecting: String = "short"

      def decodeJsonNumber(cursor: Cursor, in: JsonReader): Short =
        in.readShort()

      def decodeJsonString(cursor: Cursor, in: JsonReader): Short =
        in.readStringAsShort()

      def encodeValue(x: Short, out: JsonWriter): Unit = out.writeVal(x)

      def decodeKey(in: JsonReader): Short = in.readKeyAsShort()

      def encodeKey(x: Short, out: JsonWriter): Unit = out.writeKey(x)
    }

    val byte: JCodec[Byte] = new NumericJCodec[Byte] {
      def expecting: String = "byte"

      def decodeJsonNumber(cursor: Cursor, in: JsonReader): Byte = in.readByte()

      def decodeJsonString(cursor: Cursor, in: JsonReader): Byte = in.readByte()

      def encodeValue(x: Byte, out: JsonWriter): Unit = out.writeVal(x)

      def decodeKey(in: JsonReader): Byte = in.readKeyAsByte()

      def encodeKey(x: Byte, out: JsonWriter): Unit = out.writeKey(x)
    }

    val bytes: JCodec[Blob] =
      new JCodec[Blob] {
        def expecting: String = "byte-array" // or blob?

        override def canBeKey: Boolean = false

        def decodeValue(cursor: Cursor, in: JsonReader): Blob = Blob(
          in.readBase64AsBytes(null)
        )

        def encodeValue(x: Blob, out: JsonWriter): Unit =
          out.writeBase64Val(x.toArray, doPadding = true)

        def decodeKey(in: JsonReader): Blob =
          in.decodeError("Cannot use byte array as key")

        def encodeKey(x: Blob, out: JsonWriter): Unit =
          out.encodeError("Cannot use byte array as key")
      }

    val bigdecimal: JCodec[BigDecimal] =
      new JCodec[BigDecimal] {
        def expecting: String = "big-decimal"

        def decodeValue(cursor: Cursor, in: JsonReader): BigDecimal =
          in.readBigDecimal(null)

        def decodeKey(in: JsonReader): BigDecimal = in.readKeyAsBigDecimal()

        def encodeValue(value: BigDecimal, out: JsonWriter): Unit =
          out.writeVal(value)

        def encodeKey(value: BigDecimal, out: JsonWriter): Unit =
          out.writeVal(value)
      }

    val bigint: JCodec[BigInt] =
      new JCodec[BigInt] {
        def expecting: String = "big-int"

        def decodeValue(cursor: Cursor, in: JsonReader): BigInt =
          in.readBigInt(null)

        def decodeKey(in: JsonReader): BigInt = in.readKeyAsBigInt()

        def encodeValue(value: BigInt, out: JsonWriter): Unit =
          out.writeVal(value)

        def encodeKey(value: BigInt, out: JsonWriter): Unit =
          out.writeVal(value)
      }

    val uuid: JCodec[UUID] =
      new JCodec[UUID] {
        def expecting: String = "uuid"

        def decodeValue(cursor: Cursor, in: JsonReader): UUID =
          in.readUUID(null)

        def encodeValue(x: UUID, out: JsonWriter): Unit = out.writeVal(x)

        def decodeKey(in: JsonReader): UUID = in.readKeyAsUUID()

        def encodeKey(x: UUID, out: JsonWriter): Unit = out.writeKey(x)
      }

    val timestampDateTime: JCodec[Timestamp] = new JCodec[Timestamp] {
      val expecting: String = Timestamp.showFormat(TimestampFormat.DATE_TIME)

      def decodeValue(cursor: Cursor, in: JsonReader): Timestamp =
        Timestamp.parse(in.readString(null), TimestampFormat.DATE_TIME) match {
          case x: Some[Timestamp] => x.get
          case _                  => in.decodeError("expected " + expecting)
        }

      def encodeValue(x: Timestamp, out: JsonWriter): Unit =
        out.writeNonEscapedAsciiVal(x.format(TimestampFormat.DATE_TIME))

      def decodeKey(in: JsonReader): Timestamp =
        Timestamp.parse(in.readKeyAsString(), TimestampFormat.DATE_TIME) match {
          case x: Some[Timestamp] => x.get
          case _                  => in.decodeError("expected " + expecting)
        }

      def encodeKey(x: Timestamp, out: JsonWriter): Unit =
        out.writeNonEscapedAsciiKey(x.format(TimestampFormat.DATE_TIME))
    }

    val timestampHttpDate: JCodec[Timestamp] = new JCodec[Timestamp] {
      val expecting: String = Timestamp.showFormat(TimestampFormat.HTTP_DATE)

      def decodeValue(cursor: Cursor, in: JsonReader): Timestamp =
        Timestamp.parse(in.readString(null), TimestampFormat.HTTP_DATE) match {
          case x: Some[Timestamp] => x.get
          case _                  => in.decodeError("expected " + expecting)
        }

      def encodeValue(x: Timestamp, out: JsonWriter): Unit =
        out.writeNonEscapedAsciiVal(x.format(TimestampFormat.HTTP_DATE))

      def decodeKey(in: JsonReader): Timestamp =
        Timestamp.parse(in.readKeyAsString(), TimestampFormat.HTTP_DATE) match {
          case x: Some[Timestamp] => x.get
          case _                  => in.decodeError("expected " + expecting)
        }

      def encodeKey(x: Timestamp, out: JsonWriter): Unit =
        out.writeNonEscapedAsciiKey(x.format(TimestampFormat.HTTP_DATE))
    }

    val timestampEpochSeconds: JCodec[Timestamp] = new JCodec[Timestamp] {
      val expecting: String =
        Timestamp.showFormat(TimestampFormat.EPOCH_SECONDS)

      def decodeValue(cursor: Cursor, in: JsonReader): Timestamp = {
        val timestamp = in.readBigDecimal(null)
        val epochSeconds =
          timestamp.setScale(0, BigDecimal.RoundingMode.FLOOR).toLong
        Timestamp(epochSeconds, ((timestamp - epochSeconds) * 1000000000).toInt)
      }

      def encodeValue(x: Timestamp, out: JsonWriter): Unit = {
        // TODO: can be improved with out.writeTimestampVal(x.epochSecond, x.nano) when https://github.com/plokhotnyuk/jsoniter-scala/releases/tag/v2.32.0 is used
        out.writeVal(BigDecimal({
          val es = java.math.BigDecimal.valueOf(x.epochSecond)
          if (x.nano == 0) es
          else
            es.add(
              java.math.BigDecimal.valueOf(x.nano.toLong, 9).stripTrailingZeros
            )
        }))
      }

      def decodeKey(in: JsonReader): Timestamp = {
        val timestamp = in.readKeyAsBigDecimal()
        val epochSecond = timestamp.toLong
        Timestamp(epochSecond, ((timestamp - epochSecond) * 1000000000).toInt)
      }

      def encodeKey(x: Timestamp, out: JsonWriter): Unit =
        out.writeKey(BigDecimal(x.epochSecond) + x.nano / 1000000000.0)
    }

    val unit: JCodec[Unit] =
      new JCodec[Unit] {
        def expecting: String = "empty object"

        override def canBeKey: Boolean = false

        def decodeValue(cursor: Cursor, in: JsonReader): Unit =
          if (!in.isNextToken('{') || !in.isNextToken('}'))
            in.decodeError("Expected empty object")

        def encodeValue(x: Unit, out: JsonWriter): Unit = {
          out.writeObjectStart()
          out.writeObjectEnd()
        }

        def decodeKey(in: JsonReader): Unit =
          in.decodeError("Cannot use Unit as keys")

        def encodeKey(x: Unit, out: JsonWriter): Unit =
          out.encodeError("Cannot use Unit as keys")
      }

    val localDate: JCodec[LocalDate] = new JCodec[LocalDate] {
      def expecting: String = "localDate"

      def decodeValue(cursor: Cursor, in: JsonReader): LocalDate =
        LocalDate.parseUnsafe(in.readString(null))

      def encodeValue(x: LocalDate, out: JsonWriter): Unit =
        out.writeNonEscapedAsciiVal(x.toString())

      def decodeKey(in: JsonReader): LocalDate =
        LocalDate.parseUnsafe(in.readKeyAsString())

      def encodeKey(x: LocalDate, out: JsonWriter): Unit =
        out.writeNonEscapedAsciiKey(x.toString)
    }

    val localTime: JCodec[LocalTime] = new JCodec[LocalTime] {
      def expecting: String = "localTime"

      def decodeValue(cursor: Cursor, in: JsonReader): LocalTime =
        LocalTime.parseUnsafe(in.readString(null))

      def encodeValue(x: LocalTime, out: JsonWriter): Unit =
        out.writeNonEscapedAsciiVal(x.toString())

      def decodeKey(in: JsonReader): LocalTime =
        LocalTime.parseUnsafe(in.readKeyAsString())

      def encodeKey(x: LocalTime, out: JsonWriter): Unit =
        out.writeNonEscapedAsciiKey(x.toString)
    }

    val duration: JCodec[Duration] = new JCodec[Duration] {
      def expecting: String = "duration"

      def decodeValue(cursor: Cursor, in: JsonReader): Duration =
        DurationOps.fromBigDecimal(in.readBigDecimal(null))

      def encodeValue(x: Duration, out: JsonWriter): Unit =
        out.writeVal(x.toBigDecimal)

      def decodeKey(in: JsonReader): Duration =
        DurationOps.fromBigDecimal(in.readKeyAsBigDecimal())

      def encodeKey(x: Duration, out: JsonWriter): Unit =
        out.writeKey(x.toBigDecimal)
    }

    val offsetDateTime: JCodec[OffsetDateTime] = new JCodec[OffsetDateTime] {
      def expecting: String = "offsetDateTime"

      def decodeValue(cursor: Cursor, in: JsonReader): OffsetDateTime =
        OffsetDateTime.parseUnsafe(in.readString(null))

      def encodeValue(x: OffsetDateTime, out: JsonWriter): Unit =
        out.writeNonEscapedAsciiVal(x.toString())

      def decodeKey(in: JsonReader): OffsetDateTime =
        OffsetDateTime.parseUnsafe(in.readKeyAsString())

      def encodeKey(x: OffsetDateTime, out: JsonWriter): Unit =
        out.writeNonEscapedAsciiKey(x.toString)
    }

    def document(maxArity: Int, hints: Hints): JCodec[Document] =
      new JCodec[Document] {
        import Document._
        override def canBeKey: Boolean = false

        def encodeValue(doc: Document, out: JsonWriter): Unit = doc match {
          case s: DString  => out.writeVal(s.value)
          case b: DBoolean => out.writeVal(b.value)
          case n: DNumber  => out.writeVal(n.value)
          case a: DArray =>
            out.writeArrayStart()
            a.value match {
              // short-circuiting on empty arrays to avoid the downcast to array of documents
              // which has proven to be dangerous in Scala 3:
              // https://github.com/disneystreaming/smithy4s/issues/1158
              case x: ArraySeq[_] =>
                if (x.isEmpty) ()
                else {
                  val xs = x.unsafeArray.asInstanceOf[Array[Document]]
                  var i = 0
                  while (i < xs.length) {
                    encodeValue(xs(i), out)
                    i += 1
                  }
                }
              case xs =>
                xs.foreach(encodeValue(_, out))
            }
            out.writeArrayEnd()
          case o: DObject =>
            out.writeObjectStart()
            o.value.foreach { kv =>
              out.writeKey(kv._1)
              encodeValue(kv._2, out)
            }
            out.writeObjectEnd()
          case _ => out.writeNull()
        }

        def decodeKey(in: JsonReader): Document =
          in.decodeError("Cannot use JSON document as keys")

        def encodeKey(x: Document, out: JsonWriter): Unit =
          out.encodeError("Cannot use JSON documents as keys")

        def expecting: String = "JSON document"

        private val preserveKeyOrder = hints.has(PreserveKeyOrder)
        // Borrowed from: https://github.com/plokhotnyuk/jsoniter-scala/blob/e80d51019b39efacff9e695de97dce0c23ae9135/jsoniter-scala-benchmark/src/main/scala/io/circe/CirceJsoniter.scala
        def decodeValue(cursor: Cursor, in: JsonReader): Document = {
          val b = in.nextToken()
          if (b == '"') {
            in.rollbackToken()
            new DString(in.readString(null))
          } else if (b == 'f' || b == 't') {
            in.rollbackToken()
            new DBoolean(in.readBoolean())
          } else if ((b >= '0' && b <= '9') || b == '-') {
            in.rollbackToken()
            new DNumber(in.readBigDecimal(null))
          } else if (b == '[') {
            new DArray({
              if (in.isNextToken(']')) ArraySeq.empty[Document]
              else
                ArraySeq.unsafeWrapArray {
                  in.rollbackToken()
                  var arr = new Array[Document](4)
                  var i = 0
                  while ({
                    if (i >= maxArity) maxArityError(cursor)
                    if (i == arr.length)
                      arr = java.util.Arrays.copyOf(arr, i << 1)
                    arr(i) = decodeValue(in, null)
                    i += 1
                    in.isNextToken(',')
                  }) {}
                  if (in.isCurrentToken(']')) {
                    if (i == arr.length) arr
                    else java.util.Arrays.copyOf(arr, i)
                  } else in.arrayEndOrCommaError()
                }
            })
          } else if (b == '{') {
            new DObject({
              if (in.isNextToken('}')) Map.empty
              else {
                in.rollbackToken()
                val obj =
                  if (preserveKeyOrder)
                    ListMap.newBuilder[String, Document]
                  else Map.newBuilder[String, Document]
                var i = 0
                while ({
                  // We use the maxArity limit to mitigate DoS vulnerability in default Scala `Map` implementation: https://github.com/scala/bug/issues/11203
                  if (i >= maxArity) maxArityError(cursor)
                  obj += ((in.readKeyAsString(), decodeValue(in, null)))
                  i += 1
                  in.isNextToken(',')
                }) {}
                if (in.isCurrentToken('}')) obj.result()
                else in.objectEndOrCommaError()
              }
            })
          } else in.readNullOrError(DNull, "expected JSON document")
        }

        private def maxArityError(cursor: Cursor): Nothing =
          throw cursor.payloadError(
            this,
            s"Input $expecting exceeded max arity of $maxArity"
          )
      }
  }

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): JCodec[P] = {
    tag match {
      case PBigDecimal     => PrimitiveJCodecs.bigdecimal
      case PBigInt         => PrimitiveJCodecs.bigint
      case PBlob           => PrimitiveJCodecs.bytes
      case PBoolean        => PrimitiveJCodecs.boolean
      case PByte           => PrimitiveJCodecs.byte
      case PDocument       => PrimitiveJCodecs.document(maxArity, hints)
      case PDouble         => PrimitiveJCodecs.double
      case PFloat          => PrimitiveJCodecs.float
      case PInt            => PrimitiveJCodecs.int
      case PLong           => PrimitiveJCodecs.long
      case PShort          => PrimitiveJCodecs.short
      case PString         => PrimitiveJCodecs.string
      case PTimestamp      => timestampJCodec(hints)
      case PUUID           => PrimitiveJCodecs.uuid
      case PLocalDate      => PrimitiveJCodecs.localDate
      case PLocalTime      => PrimitiveJCodecs.localTime
      case PDuration       => PrimitiveJCodecs.duration
      case POffsetDateTime => PrimitiveJCodecs.offsetDateTime
    }
  }

  def timestampJCodec(
      hints: Hints,
      defaultTimestamp: TimestampFormat = TimestampFormat.EPOCH_SECONDS
  ): JCodec[Timestamp] = {
    hints.get(TimestampFormat).getOrElse(defaultTimestamp) match {
      case TimestampFormat.DATE_TIME => PrimitiveJCodecs.timestampDateTime
      case TimestampFormat.EPOCH_SECONDS =>
        PrimitiveJCodecs.timestampEpochSeconds
      case TimestampFormat.HTTP_DATE => PrimitiveJCodecs.timestampHttpDate
    }
  }

  private def collectionImpl[C[_], A](
      tag: CollectionTag[C],
      member: Schema[A]
  ) = new JCodec[C[A]] {
    private[this] val a: JCodec[A] = apply(member)

    def expecting: String = tag.name

    override def canBeKey: Boolean = false

    def decodeValue(cursor: Cursor, in: JsonReader): C[A] =
      if (in.isNextToken('[')) {
        if (in.isNextToken(']')) tag.empty
        else {
          in.rollbackToken()
          val result = tag.build[A] { put =>
            var i = 0
            while ({
              if (i >= maxArity) maxArityError(cursor)
              cursor.push(i)
              put(cursor.decode(a, in))
              cursor.pop()
              i += 1
              in.isNextToken(',')
            }) ()
          }
          if (in.isCurrentToken(']')) result
          else in.arrayEndOrCommaError()
        }
      } else in.decodeError("Expected JSON array")

    def encodeValue(xs: C[A], out: JsonWriter): Unit = {
      out.writeArrayStart()
      tag.iterator(xs).foreach(x => a.encodeValue(x, out))
      out.writeArrayEnd()
    }

    def decodeKey(in: JsonReader): C[A] =
      in.decodeError(s"Cannot use ${tag.name} as keys")

    def encodeKey(xs: C[A], out: JsonWriter): Unit =
      out.encodeError(s"Cannot use ${tag.name} as keys")

    private[this] def maxArityError(cursor: Cursor): Nothing =
      throw cursor.payloadError(
        this,
        s"Input $expecting exceeded max arity of $maxArity"
      )
  }

  private def indexedSeq[A](
      member: Schema[A]
  ): JCodec[IndexedSeq[A]] = new JCodec[IndexedSeq[A]] {
    private[this] val a = apply(member)
    def expecting: String = "list"

    override def canBeKey: Boolean = false

    val withBuilder = CollectionTag.IndexedSeqTag.compactBuilder(member)

    def decodeValue(cursor: Cursor, in: JsonReader): IndexedSeq[A] =
      if (in.isNextToken('[')) {
        if (in.isNextToken(']')) Vector.empty
        else {
          in.rollbackToken()
          withBuilder { put =>
            var i = 0
            while ({
              if (i >= maxArity) maxArityError(cursor)
              cursor.push(i)
              put(cursor.decode(a, in))
              cursor.pop()
              i += 1
              in.isNextToken(',')
            }) ()
            if (!in.isCurrentToken(']')) {
              in.arrayEndOrCommaError()
            }
          }
        }
      } else in.decodeError("Expected JSON array")

    def encodeValue(xs: IndexedSeq[A], out: JsonWriter): Unit = {
      out.writeArrayStart()
      xs match {
        case x: ArraySeq[A] =>
          val xs = x.unsafeArray.asInstanceOf[Array[A]]
          var i = 0
          while (i < xs.length) {
            a.encodeValue(xs(i), out)
            i += 1
          }
        case _ =>
          xs.foreach(x => a.encodeValue(x, out))
      }
      out.writeArrayEnd()
    }

    def decodeKey(in: JsonReader): IndexedSeq[A] =
      in.decodeError("Cannot use vectors as keys")

    def encodeKey(xs: IndexedSeq[A], out: JsonWriter): Unit =
      out.encodeError("Cannot use vectors as keys")

    private[this] def maxArityError(cursor: Cursor): Nothing =
      throw cursor.payloadError(
        this,
        s"Input $expecting exceeded max arity of $maxArity"
      )
  }

  private def objectMap[C[_, _], K, V](
      tag: MapTag[C],
      jk: JCodec[K],
      jv: JCodec[V]
  ): JCodec[C[K, V]] = new JCodec[C[K, V]] {
    val expecting: String = "map"

    override def canBeKey: Boolean = false

    def decodeValue(cursor: Cursor, in: JsonReader): C[K, V] =
      if (in.isNextToken('{')) {
        if (in.isNextToken('}')) tag.empty
        else {
          in.rollbackToken()
          val result = tag.build[K, V] { put =>
            var i = 0
            while ({
              if (i >= maxArity) maxArityError(cursor)
              val key = jk.decodeKey(in)
              val value = {
                cursor.push(i)
                val result = cursor.decode(jv, in)
                cursor.pop()
                result
              }
              put(key, value)
              i += 1
              in.isNextToken(',')
            }) ()
          }
          if (in.isCurrentToken('}')) result
          else in.objectEndOrCommaError()
        }
      } else in.decodeError("Expected JSON object")

    def encodeValue(xs: C[K, V], out: JsonWriter): Unit = {
      out.writeObjectStart()
      tag.iterator(xs).foreach { kv =>
        jk.encodeKey(kv._1, out)
        jv.encodeValue(kv._2, out)
      }
      out.writeObjectEnd()
    }

    def decodeKey(in: JsonReader): C[K, V] =
      in.decodeError("Cannot use maps as keys")

    def encodeKey(xs: C[K, V], out: JsonWriter): Unit =
      out.encodeError("Cannot use maps as keys")

    private[this] def maxArityError(cursor: Cursor): Nothing =
      throw cursor.payloadError(
        this,
        s"Input $expecting exceeded max arity of $maxArity"
      )
  }

  private def arrayMap[C[_, _], K, V](
      tag: MapTag[C],
      k: Schema[K],
      v: Schema[V]
  ): JCodec[C[K, V]] = {
    val kField = Field.required[(K, V), K]("key", k, _._1)
    val vField = Field.required[(K, V), V]("value", v, _._2)
    val kvCodec = Schema.struct(Vector(kField, vField))(fields =>
      (fields(0).asInstanceOf[K], fields(1).asInstanceOf[V])
    )

    collectionImpl(CollectionTag.ListTag, kvCodec).biject(
      l => tag.fromIterator(l.iterator),
      tag.iterator(_).toList
    )
  }

  private def flexibleNullParsingMap[C[_, _], K, V](
      tag: MapTag[C],
      jk: JCodec[K],
      jv: JCodec[V]
  ): JCodec[C[K, V]] =
    new JCodec[C[K, V]] {
      val expecting: String = tag.name

      override def canBeKey: Boolean = false

      def decodeValue(cursor: Cursor, in: JsonReader): C[K, V] =
        if (in.isNextToken('{')) {
          if (in.isNextToken('}')) tag.empty
          else {
            in.rollbackToken()
            val result = tag.build[K, V] { put =>
              var i = 0
              while ({
                if (i >= maxArity) maxArityError(cursor)
                val key = jk.decodeKey(in)
                cursor.push(i)
                if (in.isNextToken('n')) {
                  in.readNullOrError[Unit]((), "Expected null")
                } else {
                  in.rollbackToken()
                  val value = cursor.decode(jv, in)
                  put(key, value)
                }
                cursor.pop()

                i += 1
                in.isNextToken(',')
              }) ()
            }
            if (in.isCurrentToken('}')) result
            else in.objectEndOrCommaError()
          }
        } else in.decodeError("Expected JSON object")

      def encodeValue(xs: C[K, V], out: JsonWriter): Unit = {
        out.writeObjectStart()
        tag.iterator(xs).foreach { kv =>
          jk.encodeKey(kv._1, out)
          jv.encodeValue(kv._2, out)
        }
        out.writeObjectEnd()
      }

      def decodeKey(in: JsonReader): C[K, V] =
        in.decodeError("Cannot use maps as keys")

      def encodeKey(xs: C[K, V], out: JsonWriter): Unit =
        out.encodeError("Cannot use maps as keys")

      private def maxArityError(cursor: Cursor): Nothing =
        throw cursor.payloadError(
          this,
          s"Input $expecting exceeded max arity of $maxArity"
        )
    }

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): JCodec[C[A]] = {
    tag match {
      case CollectionTag.IndexedSeqTag => indexedSeq(member)
      case x                           => collectionImpl(x, member)
    }
  }

  override def map[C[_, _], K, V](
      shapeId: ShapeId,
      hints: Hints,
      tag: MapTag[C],
      key: Schema[K],
      value: Schema[V]
  ): JCodec[C[K, V]] = {
    val jk = apply(key)
    val jv = apply(value)
    if (jk.canBeKey) {
      if (flexibleCollectionsSupport && !value.isOption)
        flexibleNullParsingMap(tag, jk, jv)
      else objectMap(tag, jk, jv)
    } else arrayMap(tag, key, value)
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): JCodec[B] =
    apply(schema).biject(bijection.toFunction, bijection.from)

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): JCodec[B] =
    apply(schema).biject(refinement.asThrowingFunction, refinement.from)

  override def lazily[A](suspend: Lazy[Schema[A]]): JCodec[A] = new JCodec[A] {
    lazy val underlying = apply(suspend.value)

    def expecting: String = underlying.expecting

    def decodeValue(cursor: Cursor, in: JsonReader): A =
      underlying.decodeValue(cursor, in)

    def encodeValue(x: A, out: JsonWriter): Unit =
      underlying.encodeValue(x, out)

    def decodeKey(in: JsonReader): A = underlying.decodeKey(in)

    def encodeKey(x: A, out: JsonWriter): Unit = underlying.encodeKey(x, out)
  }

  private type Writer[A] = A => JsonWriter => Unit

  private abstract class UnionJCodec[U](
      alternatives: Vector[Alt[U, _]],
      isDiscriminated: Boolean = false
  )(
      dispatch: Alt.Dispatcher[U]
  ) extends JCodec[U] {

    private def jsonLabel[A](alt: Alt[U, A]): String =
      alt.hints.get(JsonName) match {
        case None    => alt.label
        case Some(x) => x.value
      }

    private val handlerMap: Map[String, UnionJCodec.AltHandler[U, _]] =
      alternatives.collect {
        case alt if !alt.hints.has(JsonUnknown) =>
          jsonLabel(alt) -> UnionJCodec.AltHandler.create(alt)
      }.toMap

    private val unknownAlt =
      alternatives.find(_.hints.has(JsonUnknown)).map { alt =>
        if (isDiscriminated) {
          val handler = UnionJCodec.AltHandler.create(alt)
          (_: String) => handler
        } else UnionJCodec.AltHandler.openUnionTaggedUnknown(alt)
      }

    protected def getHandler(key: String) = handlerMap
      .get(key)
      .orElse(unknownAlt.map(_(key)))

  }

  private object UnionJCodec {

    private type DocumentTransformer[A] = (A, Document => Document) => A

    protected abstract class AltHandler[U, A] {
      def handle(cursor: Cursor, reader: JsonReader): U =
        inject(handleVariant(cursor, reader))

      protected def inject(a: A): U
      protected def handleVariant(cursor: Cursor, reader: JsonReader): A
    }

    private object AltHandler {
      def create[U, A](alt: Alt[U, A]): AltHandler[U, A] = new FromAlt(alt)

      def openUnionTaggedUnknown[U, A](
          alt: Alt[U, A]
      ): String => AltHandler[U, A] = {
        val underlying = AltHandler.create(alt)
        val documentTransformer = alt.schema.compile(TransformDocumentCompiler)
        key =>
          new AltHandler.Mapped(
            underlying,
            a => documentTransformer(a, doc => Document.obj(key -> doc))
          )
      }

      private final class FromAlt[U, A](alt: Alt[U, A])
          extends AltHandler[U, A] {

        private val codec = self.apply(alt.schema)

        protected def inject(a: A): U = alt.inject(a)
        protected def handleVariant(cursor: Cursor, reader: JsonReader): A =
          cursor.decode(codec, reader)
      }

      private final class Mapped[U, A](
          underlying: AltHandler[U, A],
          map: A => A
      ) extends AltHandler[U, A] {
        protected def inject(a: A): U = underlying.inject(a)
        protected def handleVariant(cursor: Cursor, reader: JsonReader): A =
          map(underlying.handleVariant(cursor, reader))

      }

    }

    private object TransformDocumentCompiler
        extends SchemaVisitor.Default[DocumentTransformer] {
      override def default[A]: DocumentTransformer[A] = (a, _) => a

      override def primitive[P](
          shapeId: ShapeId,
          hints: Hints,
          tag: Primitive[P]
      ): DocumentTransformer[P] = tag match {
        case PDocument => (a, f) => f(a)
        case _         => default
      }

      override def biject[A, B](
          schema: Schema[A],
          bijection: Bijection[A, B]
      ): DocumentTransformer[B] = {
        val compiled = schema.compile(this)
        (b, f) => bijection.to(compiled(bijection.from(b), f))
      }
    }
  }

  private final class TaggedUnionJCodec[U](alternatives: Vector[Alt[U, _]])(
      dispatch: Alt.Dispatcher[U],
      isLenient: Boolean
  ) extends UnionJCodec[U](alternatives)(dispatch) {

    val expecting = "tagged-union"

    override def canBeKey: Boolean = false

    protected val precompiler = new smithy4s.schema.Alt.Precompiler[Writer] {
      def apply[A](label: String, instance: Schema[A]): Writer[A] = {
        val jcodecA = instance.compile(self)

        if (!instance.hints.has(JsonUnknown)) {
          val key = instance.hints.get(JsonName).map(_.value).getOrElse(label)
          a =>
            out => {
              out.writeObjectStart()
              out.writeKey(key)
              jcodecA.encodeValue(a, out)
              out.writeObjectEnd()
            }
        } else { a => out =>
          jcodecA.encodeValue(a, out)
        }
      }
    }
    protected val writer = dispatch.compile(precompiler)

    def encodeValue(u: U, out: JsonWriter): Unit = {
      writer(u)(out)
    }

    def decodeKey(in: JsonReader): U =
      in.decodeError("Cannot use coproducts as keys")

    def encodeKey(u: U, out: JsonWriter): Unit =
      out.encodeError("Cannot use coproducts as keys")

    def decodeValue(cursor: Cursor, in: JsonReader): U = {
      var result: U = null.asInstanceOf[U]
      var lastKey: String = null.asInstanceOf[String]

      def readKey(): Unit = {
        lastKey = in.readKeyAsString()
        cursor.push(lastKey)
        if (isLenient && in.isNextToken('n')) {
          in.readNullOrError((), "expected null")
        } else if (result == null) {
          if (isLenient) in.rollbackToken()
          getHandler(lastKey) match {
            case Some(handler) => result = handler.handle(cursor, in)
            case None          => onUnknownDiscriminator(in, lastKey)
          }
        } else {
          in.decodeError(emptyObjectErrorMessage)
        }

        cursor.pop()
      }

      if (in.isNextToken('{')) {
        if (in.isNextToken('}'))
          in.decodeError(emptyObjectErrorMessage)
        else {
          in.rollbackToken()

          readKey()

          if (isLenient) {
            while (in.isNextToken(',')) {
              readKey()
            }
            in.rollbackToken()
          }

          if (in.isNextToken('}')) {
            if (result == null)
              in.decodeError("Expected a single non-null value")
            else
              result
          } else {
            if (isLenient) in.objectEndOrCommaError()
            else in.decodeError(s"Expected no other field after '$lastKey'")
          }

        }
      } else in.decodeError("Expected JSON object")
    }

    private def emptyObjectErrorMessage: String =
      if (isLenient) "Expected a single non-null value"
      else "Expected a single key/value pair"

    private def onUnknownDiscriminator(in: JsonReader, key: String): Unit =
      if (isLenient) in.skip() else in.discriminatorValueError(key)
  }

  private def taggedUnion[U](
      alternatives: Vector[Alt[U, _]]
  )(dispatch: Alt.Dispatcher[U]): JCodec[U] =
    new TaggedUnionJCodec[U](alternatives)(dispatch, isLenient = false)

  private def lenientTaggedUnion[U](
      alternatives: Vector[Alt[U, _]]
  )(dispatch: Alt.Dispatcher[U]): JCodec[U] =
    new TaggedUnionJCodec[U](alternatives)(dispatch, isLenient = true)

  private def untaggedUnion[U](
      alternatives: Vector[Alt[U, _]]
  )(dispatch: Alt.Dispatcher[U]): JCodec[U] = new JCodec[U] {
    def expecting: String = "untaggedUnion"

    override def canBeKey: Boolean = false

    private[this] val handlerList: Array[(Cursor, JsonReader) => U] = {
      val res = Array.newBuilder[(Cursor, JsonReader) => U]

      def handler[A](alt: Alt[U, A]) = {
        val codec = apply(alt.schema)
        (cursor: Cursor, reader: JsonReader) =>
          alt.inject(cursor.decode(codec, reader))
      }

      alternatives.foreach(alt => res += handler(alt))
      res.result()
    }

    def decodeValue(cursor: Cursor, in: JsonReader): U = {
      var z: U = null.asInstanceOf[U]
      val len = handlerList.length
      var i = 0
      while (z == null && i < len) {
        in.setMark()
        val handler = handlerList(i)
        try {
          z = handler(cursor, in)
        } catch {
          case _: Throwable =>
            in.rollbackToMark()
            i += 1
        }
      }
      if (z != null) z
      else cursor.payloadError(this, "Could not decode untagged union")
    }

    val precompiler = new smithy4s.schema.Alt.Precompiler[Writer] {
      def apply[A](label: String, instance: Schema[A]): Writer[A] = {
        val jcodecA = instance.compile(self)
        a => out => jcodecA.encodeValue(a, out)
      }
    }
    val writer = dispatch.compile(precompiler)

    def encodeValue(u: U, out: JsonWriter): Unit = {
      writer(u)(out)
    }

    def decodeKey(in: JsonReader): U =
      in.decodeError("Cannot use coproducts as keys")

    def encodeKey(u: U, out: JsonWriter): Unit =
      out.encodeError("Cannot use coproducts as keys")
  }

  private def discriminatedUnion[U](
      alternatives: Vector[Alt[U, _]],
      discriminated: Discriminated
  )(dispatch: Alt.Dispatcher[U]): JCodec[U] =
    new UnionJCodec[U](alternatives, isDiscriminated = true)(dispatch) {
      def expecting: String = "discriminated-union"

      override def canBeKey: Boolean = false

      def decodeValue(cursor: Cursor, in: JsonReader): U =
        if (in.isNextToken('{')) {
          in.setMark()
          if (in.skipToKey(discriminated.value)) {
            val key = in.readString("")
            in.rollbackToMark()
            in.rollbackToken()
            cursor.push(key)
            getHandler(key) match {
              case Some(handler) =>
                val result = handler.handle(cursor, in)
                cursor.pop()
                result
              case None => in.discriminatorValueError(key)
            }
          } else
            in.decodeError(
              s"Unable to find discriminator ${discriminated.value}"
            )
        } else in.decodeError("Expected JSON object")

      val precompiler = new smithy4s.schema.Alt.Precompiler[Writer] {
        def apply[A](label: String, instance: Schema[A]): Writer[A] = {
          val jsonLabel =
            instance.hints.get(JsonName).map(_.value).getOrElse(label)
          val jcodecA = instance
            .addHints(
              Hints(DiscriminatedUnionMember(discriminated.value, jsonLabel))
            )
            .compile(self)
          a => out => jcodecA.encodeValue(a, out)
        }
      }
      val writer = dispatch.compile(precompiler)

      def encodeValue(u: U, out: JsonWriter): Unit = {
        writer(u)(out)
      }

      def decodeKey(in: JsonReader): U =
        in.decodeError("Cannot use coproducts as keys")

      def encodeKey(x: U, out: JsonWriter): Unit =
        out.encodeError("Cannot use coproducts as keys")
    }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[Alt[U, _]],
      dispatch: Alt.Dispatcher[U]
  ): JCodec[U] = hints match {
    case Untagged.hint(_)      => untaggedUnion(alternatives)(dispatch)
    case Discriminated.hint(d) => discriminatedUnion(alternatives, d)(dispatch)
    case _ =>
      if (lenientTaggedUnionDecoding) lenientTaggedUnion(alternatives)(dispatch)
      else taggedUnion(alternatives)(dispatch)
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag[E],
      values: List[EnumValue[E]]
  ): JCodec[E] =
    tag match {
      case t: EnumTag.IntEnum[E] =>
        handleIntEnum(shapeId, hints, values, t)
      case t: EnumTag.StringEnum[E] =>
        handleStringEnum(shapeId, hints, values, t)
    }

  private def handleStringEnum[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      tag: EnumTag.StringEnum[E]
  ): JCodec[E] = new JCodec[E] {
    private val stringValueMap: Map[String, E] =
      values.map(v => v.stringValue -> v.value).toMap

    val expecting: String =
      s"enumeration: [${values.map(_.stringValue).mkString(", ")}]"

    private val decode: (JsonReader, String) => E = tag.unknown match {
      case Some(unknown) =>
        (_, str) => stringValueMap.getOrElse(str, unknown(str))
      case None =>
        (in, str) =>
          stringValueMap.get(str) match {
            case Some(value) => value
            case None        => in.enumValueError(str)
          }
    }

    def decodeValue(cursor: Cursor, in: JsonReader): E = {
      val str = in.readString(null)
      decode(in, str)
    }

    def encodeValue(x: E, out: JsonWriter): Unit =
      out.writeVal(tag.value(x))

    def decodeKey(in: JsonReader): E = {
      val str = in.readKeyAsString()
      decode(in, str)
    }

    def encodeKey(x: E, out: JsonWriter): Unit =
      out.writeKey(tag.value(x))
  }

  private def handleIntEnum[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      tag: EnumTag.IntEnum[E]
  ): JCodec[E] = new JCodec[E] {
    private val intValueMap: Map[Int, E] =
      values.map(v => v.intValue -> v.value).toMap

    val expecting: String =
      s"enumeration: [${values.map(_.stringValue).mkString(", ")}]"

    private val decode: (JsonReader, Int) => E = tag.unknown match {
      case Some(unknown) =>
        (_, i) => intValueMap.getOrElse(i, unknown(i))
      case None =>
        (in, i) =>
          intValueMap.get(i) match {
            case Some(value) => value
            case None        => in.enumValueError(i)
          }
    }

    def decodeValue(cursor: Cursor, in: JsonReader): E = {
      val i = in.readInt()
      decode(in, i)
    }

    def encodeValue(x: E, out: JsonWriter): Unit =
      out.writeVal(tag.value(x))

    def decodeKey(in: JsonReader): E = {
      val i = in.readKeyAsInt()
      decode(in, i)
    }

    def encodeKey(x: E, out: JsonWriter): Unit =
      out.writeKey(tag.value(x))
  }

  override def option[C[_], A](
      tag: OptionalTag[C],
      schema: Schema[A]
  ): JCodec[C[A]] =
    new JCodec[C[A]] {
      val underlying: JCodec[A] = self(schema)
      val aIsNullable =
        schema.hints.has(Nullable) && schema.isOption
      def expecting: String = s"JsNull or ${underlying.expecting}"
      def decodeKey(in: JsonReader): C[A] = ???
      def encodeKey(x: C[A], out: JsonWriter): Unit = ???
      def encodeValue(x: C[A], out: JsonWriter): Unit =
        tag.toScalaOption(x) match {
          case None        => out.writeNull()
          case Some(value) => underlying.encodeValue(value, out)
        }

      def decodeValue(cursor: Cursor, in: JsonReader): C[A] =
        // if `A` is an option and has nullable, we delegate the handling of `null` to it.
        // This allows for supporting Json-merge patches, where the absence of value
        // and the presence of "null" have different meanings.
        if (in.isNextToken('n') && !aIsNullable)
          tag.fromScalaOption(
            in.readNullOrError[Option[A]](None, "Expected null")
          )
        else {
          in.rollbackToken()
          tag.some(underlying.decodeValue(cursor, in))
        }
    }

  private def jsonLabel[A, Z](field: Field[Z, A]): String =
    field.hints.get(JsonName) match {
      case None    => field.label
      case Some(x) => x.value
    }

  private def isForJsonUnknown[Z, A](field: Field[Z, A]): Boolean =
    field.hints.has(JsonUnknown)

  private type Handler = (Cursor, JsonReader, util.HashMap[String, Any]) => Unit

  private def fieldHandler[Z, A](
      field: Field[Z, A],
      // nullable A
      default: Any
  ): Handler = {
    val codec = apply(field.schema)
    val label = field.label

    val decodeFn: (Cursor, JCodec[A], JsonReader) => A = {
      val allowExplicitNulls =
        ! {
          // required fields can't accept explicit nulls
          field.hints.has(Required) ||
          // if there was no default, we'd allow explicit nulls by virtue of having an OptionSchema
          default == null ||
          // nullables have separate handling in OptionSchema
          field.hints.has(alloy.Nullable)
        }

      if (allowExplicitNulls)
        (cursor, codec, in) =>
          if (in.isNextToken('n')) {
            in.readNullOrError(
              default.asInstanceOf[A],
              s"Expected null for field $label"
            )
          } else {
            in.rollbackToken()
            cursor.decode(codec, in)
          }
      else
        _.decode(_, _)
    }

    (cursor, in, mmap) =>
      val _ = mmap.put(
        label, {
          cursor.push(label)
          val result = decodeFn(cursor, codec, in)
          cursor.pop()
          result
        }
      )
  }

  private def writeLabel(label: String, out: JsonWriter): Unit =
    if (label.forall(JsonWriter.isNonEscapedAscii)) {
      out.writeNonEscapedAsciiKey(label)
    } else out.writeKey(label)

  private def fieldEncoder[Z, A](
      field: Field[Z, A]
  ): (Z, JsonWriter) => Unit = {
    val codec = apply(field.schema)
    val jLabel = jsonLabel(field)
    val shouldRender = fieldFilter.compile(field)
    (z: Z, out: JsonWriter) =>
      val a = field.get(z)
      if (shouldRender(a)) {
        writeLabel(jLabel, out)
        codec.encodeValue(a, out)
      }
  }

  private def jsonUnknownFieldEncoder[Z, A](
      field: Field[Z, A]
  ): (Z, JsonWriter) => Unit = {
    val docEncoder = Document.Encoder.fromSchema(field.schema)
    (z: Z, out: JsonWriter) =>
      field.foreachUnlessDefault(z) { a =>
        docEncoder.encode(a) match {
          case Document.DObject(value) =>
            value.foreach { case (label: String, value: Document) =>
              writeLabel(label, out)
              PrimitiveJCodecs
                .document(maxArity, field.hints)
                .encodeValue(value, out)
            }
          case _ =>
            out.encodeError(
              s"Failed encoding field ${field.label} because it cannot be converted to a JSON object"
            )
        }
      }
  }

  private type Fields[Z] = Vector[Field[Z, _]]
  private type LabelledFields[Z] = Vector[(Field[Z, _], String, Any)]
  private def labelledFields[Z](fields: Fields[Z]): LabelledFields[Z] =
    fields.map { field =>
      val jLabel = jsonLabel(field)
      val decoded: Option[Any] = field.schema.getDefaultValue
      val default = decoded.orNull
      (field, jLabel, default)
    }

  private def structRetainUnknownFields[Z](
      allFields: LabelledFields[Z],
      knownFields: LabelledFields[Z],
      fieldsForUnknown: LabelledFields[Z],
      structHints: Hints
  )(
      const: Vector[Any] => Z,
      encode: (Z, JsonWriter, Vector[(Z, JsonWriter) => Unit]) => Unit
  ): JCodec[Z] =
    new JCodec[Z] {

      private val fieldForUnknownDocumentDecoders = fieldsForUnknown.map {
        case (field, label, _) =>
          label -> Document.Decoder
            .fromSchema(field.schema)
            .asInstanceOf[Document.Decoder[Any]]
      }.toMap

      private[this] val handlers =
        new util.HashMap[String, Handler](knownFields.length << 1, 0.5f) {
          knownFields.foreach { case (field, jLabel, default) =>
            put(jLabel, fieldHandler(field, default))
          }
        }

      private[this] val documentEncoders =
        knownFields.map(labelledField => fieldEncoder(labelledField._1)) ++
          fieldsForUnknown.map(f => jsonUnknownFieldEncoder(f._1))

      def expecting: String = "object"

      override def canBeKey = false

      def decodeValue(cursor: Cursor, in: JsonReader): Z =
        decodeValue_(cursor, in)(emptyMetadata)

      private def decodeValue_(
          cursor: Cursor,
          in: JsonReader
      ): scala.collection.Map[String, Any] => Z = {
        val unknownValues = ListBuffer[(String, Document)]()
        val buffer = new util.HashMap[String, Any](handlers.size << 1, 0.5f)
        if (in.isNextToken('{')) {
          if (!in.isNextToken('}')) {
            in.rollbackToken()
            while ({
              val key = in.readKeyAsString()
              val handler = handlers.get(key)
              if (handler eq null) {
                val value = PrimitiveJCodecs
                  .document(maxArity, Hints.empty)
                  .decodeValue(cursor, in)
                unknownValues += (key -> value)
              } else handler(cursor, in, buffer)
              in.isNextToken(',')
            }) ()
            if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
          }
        } else in.decodeError("Expected JSON object")

        // At this point, we have parsed the json and retrieved
        // all the values that interest us for the construction
        // of our domain object.
        // We re-order the values following the order of the schema
        // fields before calling the constructor.
        { (meta: scala.collection.Map[String, Any]) =>
          meta.foreach(kv => buffer.put(kv._1, kv._2))
          val stage2 = new VectorBuilder[Any]
          val unknownValue =
            if (unknownValues.nonEmpty) Document.obj(unknownValues) else null

          allFields.foreach { case (f, jsonLabel, default) =>
            stage2 += {
              fieldForUnknownDocumentDecoders.get(jsonLabel) match {
                case None =>
                  val value = buffer.get(f.label)
                  if (value == null) {
                    if (default == null)
                      cursor.requiredFieldError(jsonLabel, jsonLabel)
                    else default
                  } else value

                case Some(docDecoder) =>
                  if (unknownValue == null) {
                    if (default == null) {
                      docDecoder
                        .decode(Document.obj())
                        .getOrElse(
                          in.decodeError(
                            s"${cursor.getPath(Nil)} Failed translating a Document.DObject to the type targeted by ${f.label}."
                          )
                        )
                    } else default
                  } else {
                    docDecoder
                      .decode(unknownValue)
                      .getOrElse(
                        in.decodeError(
                          s"${cursor.getPath(Nil)} Failed translating a Document.DObject to the type targeted by ${f.label}."
                        )
                      )
                  }
              }
            }
          }
          const(stage2.result())
        }
      }

      def encodeValue(z: Z, out: JsonWriter): Unit =
        encode(z, out, documentEncoders)

      def decodeKey(in: JsonReader): Z =
        in.decodeError("Cannot use products as keys")

      def encodeKey(x: Z, out: JsonWriter): Unit =
        out.encodeError("Cannot use products as keys")
    }

  private def structIgnoreUnknownFields[Z](
      fields: LabelledFields[Z],
      structHints: Hints
  )(
      const: Vector[Any] => Z,
      encode: (Z, JsonWriter, Vector[(Z, JsonWriter) => Unit]) => Unit
  ): JCodec[Z] =
    new JCodec[Z] {

      private[this] val handlers =
        new util.HashMap[String, Handler](fields.length << 1, 0.5f) {
          fields.foreach { case (field, jLabel, default) =>
            put(jLabel, fieldHandler(field, default))
          }
        }

      private[this] val documentEncoders =
        fields.map(labelledField => fieldEncoder(labelledField._1))

      def expecting: String = "object"

      override def canBeKey = false

      def decodeValue(cursor: Cursor, in: JsonReader): Z =
        decodeValue_(cursor, in)(emptyMetadata)

      private def decodeValue_(
          cursor: Cursor,
          in: JsonReader
      ): scala.collection.Map[String, Any] => Z = {
        val buffer = new util.HashMap[String, Any](handlers.size << 1, 0.5f)
        if (in.isNextToken('{')) {
          if (!in.isNextToken('}')) {
            in.rollbackToken()
            while ({
              val handler = handlers.get(in.readKeyAsString())
              if (handler eq null) in.skip()
              else handler(cursor, in, buffer)
              in.isNextToken(',')
            }) ()
            if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
          }
        } else in.decodeError("Expected JSON object")

        // At this point, we have parsed the json and retrieved
        // all the values that interest us for the construction
        // of our domain object.
        // We re-order the values following the order of the schema
        // fields before calling the constructor.
        { (meta: scala.collection.Map[String, Any]) =>
          meta.foreach(kv => buffer.put(kv._1, kv._2))
          val stage2 = new VectorBuilder[Any]
          fields.foreach { case (f, jsonLabel, default) =>
            stage2 += {
              val value = buffer.get(f.label)
              if (value == null) {
                if (default == null)
                  cursor.requiredFieldError(jsonLabel, jsonLabel)
                else default
              } else value
            }
          }
          const(stage2.result())
        }
      }

      def encodeValue(z: Z, out: JsonWriter): Unit =
        encode(z, out, documentEncoders)

      def decodeKey(in: JsonReader): Z =
        in.decodeError("Cannot use products as keys")

      def encodeKey(x: Z, out: JsonWriter): Unit =
        out.encodeError("Cannot use products as keys")
    }

  private def nonPayloadStruct[Z](
      fields: LabelledFields[Z],
      structHints: Hints
  )(
      const: Vector[Any] => Z,
      encode: (Z, JsonWriter, Vector[(Z, JsonWriter) => Unit]) => Unit
  ): JCodec[Z] = {
    val (fieldsForUnknown, knownFields) = fields.partition {
      case (field, _, _) => isForJsonUnknown(field)
    }

    if (fieldsForUnknown.isEmpty)
      structIgnoreUnknownFields(fields, structHints)(const, encode)
    else
      structRetainUnknownFields(
        fields,
        knownFields,
        fieldsForUnknown,
        structHints
      )(const, encode)
  }

  private def basicStruct[A, S](
      fields: LabelledFields[S],
      structHints: Hints
  )(make: Vector[Any] => S): JCodec[S] = {
    val encode = {
      (
          z: S,
          out: JsonWriter,
          documentEncoders: Vector[(S, JsonWriter) => Unit]
      ) =>
        out.writeObjectStart()
        documentEncoders.foreach(encoder => encoder(z, out))
        out.writeObjectEnd()
    }

    nonPayloadStruct(fields, structHints)(make, encode)
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): JCodec[S] = {
    val lFields = labelledFields[S](fields)
    hints match {
      case DiscriminatedUnionMember.hint(d) =>
        val encode =
          if (
            d.propertyName.forall(JsonWriter.isNonEscapedAscii) &&
            d.alternativeLabel.forall(JsonWriter.isNonEscapedAscii)
          ) {
            (
                z: S,
                out: JsonWriter,
                documentEncoders: Vector[(S, JsonWriter) => Unit]
            ) =>
              out.writeObjectStart()
              out.writeNonEscapedAsciiKey(d.propertyName)
              out.writeNonEscapedAsciiVal(d.alternativeLabel)
              documentEncoders.foreach(encoder => encoder(z, out))
              out.writeObjectEnd()
          } else {
            (
                z: S,
                out: JsonWriter,
                documentEncoders: Vector[(S, JsonWriter) => Unit]
            ) =>
              out.writeObjectStart()
              out.writeKey(d.propertyName)
              out.writeVal(d.alternativeLabel)
              documentEncoders.foreach(encoder => encoder(z, out))
              out.writeObjectEnd()
          }
        nonPayloadStruct(lFields, hints)(make, encode)
      case _ =>
        basicStruct(lFields, hints)(make)
    }
  }
}
