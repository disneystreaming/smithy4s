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
package http
package json

import java.util.UUID
import java.util

import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader
import com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter
import smithy.api.HttpPayload
import smithy.api.JsonName
import smithy.api.TimestampFormat
import smithy4s.api.Discriminated
import smithy4s.api.Untagged
import smithy4s.internals.DiscriminatedUnionMember
import smithy4s.internals.InputOutput
import smithy4s.schema._
import smithy4s.schema.Primitive._
import smithy4s.Timestamp

import scala.collection.compat.immutable.ArraySeq
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{Map => MMap}

private[smithy4s] class SchemaVisitorJCodec(maxArity: Int)
    extends SchemaVisitor[JCodec] { self =>
  private val emptyMetadata: MMap[String, Any] = MMap.empty

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

    val int: JCodec[Int] =
      new JCodec[Int] {
        def expecting: String = "int"

        def decodeValue(cursor: Cursor, in: JsonReader): Int = in.readInt()

        def encodeValue(x: Int, out: JsonWriter): Unit = out.writeVal(x)

        def decodeKey(in: JsonReader): Int = in.readKeyAsInt()

        def encodeKey(x: Int, out: JsonWriter): Unit = out.writeKey(x)
      }

    val long: JCodec[Long] =
      new JCodec[Long] {
        def expecting: String = "long"

        def decodeValue(cursor: Cursor, in: JsonReader): Long = in.readLong()

        def encodeValue(x: Long, out: JsonWriter): Unit = out.writeVal(x)

        def decodeKey(in: JsonReader): Long = in.readKeyAsLong()

        def encodeKey(x: Long, out: JsonWriter): Unit = out.writeKey(x)
      }

    val float: JCodec[Float] =
      new JCodec[Float] {
        def expecting: String = "float"

        def decodeValue(cursor: Cursor, in: JsonReader): Float = in.readFloat()

        def encodeValue(x: Float, out: JsonWriter): Unit = out.writeVal(x)

        def decodeKey(in: JsonReader): Float = in.readKeyAsFloat()

        def encodeKey(x: Float, out: JsonWriter): Unit = out.writeKey(x)
      }

    val double: JCodec[Double] =
      new JCodec[Double] {
        def expecting: String = "double"

        def decodeValue(cursor: Cursor, in: JsonReader): Double =
          in.readDouble()

        def encodeValue(x: Double, out: JsonWriter): Unit = out.writeVal(x)

        def decodeKey(in: JsonReader): Double = in.readKeyAsDouble()

        def encodeKey(x: Double, out: JsonWriter): Unit = out.writeKey(x)
      }

    val short: JCodec[Short] =
      new JCodec[Short] {
        def expecting: String = "short"

        def decodeValue(cursor: Cursor, in: JsonReader): Short = in.readShort()

        def encodeValue(x: Short, out: JsonWriter): Unit = out.writeVal(x)

        def decodeKey(in: JsonReader): Short = in.readKeyAsShort()

        def encodeKey(x: Short, out: JsonWriter): Unit = out.writeKey(x)
      }

    val byte: JCodec[Byte] =
      new JCodec[Byte] {
        def expecting: String = "byte"

        def decodeValue(cursor: Cursor, in: JsonReader): Byte = in.readByte()

        def encodeValue(x: Byte, out: JsonWriter): Unit = out.writeVal(x)

        def decodeKey(in: JsonReader): Byte = in.readKeyAsByte()

        def encodeKey(x: Byte, out: JsonWriter): Unit = out.writeKey(x)
      }

    val bytes: JCodec[ByteArray] =
      new JCodec[ByteArray] {
        def expecting: String = "byte-array" // or blob?

        override def canBeKey: Boolean = false

        def decodeValue(cursor: Cursor, in: JsonReader): ByteArray = ByteArray(
          in.readBase64AsBytes(null)
        )

        def encodeValue(x: ByteArray, out: JsonWriter): Unit =
          out.writeBase64Val(x.array, doPadding = true)

        def decodeKey(in: JsonReader): ByteArray =
          in.decodeError("Cannot use byte array as key")

        def encodeKey(x: ByteArray, out: JsonWriter): Unit =
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

    private def forFormat[T](
        name: String,
        json: JsonInOut[T, _]
    ) =
      new JCodec[Timestamp] {
        val expecting: String = name

        def decodeValue(cursor: Cursor, in: JsonReader): Timestamp =
          json.decodeValue(in)

        def encodeValue(x: Timestamp, out: JsonWriter): Unit =
          json.encodeValue(x, out)

        // key decoding/encoding ignores the format and support only epoch
        def decodeKey(in: JsonReader): Timestamp =
          Timestamp.fromEpochSecond(in.readKeyAsLong())
        def encodeKey(x: Timestamp, out: JsonWriter): Unit =
          out.writeKey(x.epochSecond)
      }

    private trait JsonInOut[T, Out] {
      def decodeValue(in: JsonReader): Timestamp
      def encodeValue(x: Timestamp, out: JsonWriter): Unit =
        toOut(toRaw(x), out)

      protected def toRaw(ts: Timestamp): T
      protected def fromRaw(t: T): Out
      protected def fromIn(in: JsonReader): T
      protected def toOut(value: T, out: JsonWriter): Unit
    }
    private val longJsonInOut = new JsonInOut[Long, Timestamp] {
      def decodeValue(in: JsonReader): Timestamp =
        fromRaw(fromIn(in))
      def toRaw(ts: Timestamp): Long = ts.epochSecond
      def fromRaw(t: Long): Timestamp = Timestamp.fromEpochSecond(t)
      def fromIn(in: JsonReader): Long = in.readLong()
      def toOut(value: Long, out: JsonWriter): Unit = out.writeVal(value)
    }
    private def stringJsonInOut(format: TimestampFormat) =
      new JsonInOut[String, Option[Timestamp]] {
        def decodeValue(in: JsonReader): Timestamp =
          fromRaw(fromIn(in)) match {
            case Some(value) => value
            case None        => in.decodeError("Wrong timestamp format")
          }

        def toRaw(ts: Timestamp): String = ts.format(format)
        def fromRaw(t: String): Option[Timestamp] = Timestamp.parse(t, format)
        def fromIn(in: JsonReader): String = in.readString(null)
        def toOut(value: String, out: JsonWriter): Unit = out.writeVal(value)
      }

    private val dateTimeFormat = stringJsonInOut(TimestampFormat.DATE_TIME)
    val timestampDateTime = forFormat[String](
      Timestamp.showFormat(TimestampFormat.DATE_TIME),
      dateTimeFormat
    )
    private val httpDateFormat = stringJsonInOut(TimestampFormat.HTTP_DATE)
    val timestampHttpDate = forFormat[String](
      Timestamp.showFormat(TimestampFormat.HTTP_DATE),
      httpDateFormat
    )

    val timestampEpoch = forFormat[Long](
      Timestamp.showFormat(TimestampFormat.EPOCH_SECONDS),
      longJsonInOut
    )

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

    def document(maxArity: Int): JCodec[Document] = new JCodec[Document] {
      import Document._
      override def canBeKey: Boolean = false

      def encodeValue(doc: Document, out: JsonWriter): Unit = doc match {
        case s: DString  => out.writeVal(s.value)
        case b: DBoolean => out.writeVal(b.value)
        case n: DNumber  => out.writeVal(n.value)
        case a: DArray =>
          out.writeArrayStart()
          a.value.foreach(encodeValue(_, out))
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
            if (in.isNextToken(']')) IndexedSeq.empty[Document]
            else {
              in.rollbackToken()
              var arr = new Array[Document](4)
              var i = 0
              while ({
                if (i >= maxArity) maxArityError(cursor)
                if (i == arr.length) arr = java.util.Arrays.copyOf(arr, i << 1)
                arr(i) = decodeValue(in, null)
                i += 1
                in.isNextToken(',')
              }) {}

              if (in.isCurrentToken(']')) ArraySeq.unsafeWrapArray {
                if (i == arr.length) arr
                else java.util.Arrays.copyOf(arr, i)
              }
              else in.arrayEndOrCommaError()
            }
          })
        } else if (b == '{') {
          new DObject({
            if (in.isNextToken('}')) Map.empty
            else {
              in.rollbackToken()
              // We use the maxArity limit to mitigate DoS vulnerability in default Scala `Map` implementation: https://github.com/scala/bug/issues/11203
              val obj = Map.newBuilder[String, Document]
              var i = 0
              while ({
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
          s"input $expecting exceeded max arity of `$maxArity`"
        )
    }
  }

  private val documentJCodec = PrimitiveJCodecs.document(maxArity)
  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): JCodec[P] = {
    tag match {
      case PBigDecimal => PrimitiveJCodecs.bigdecimal
      case PBigInt     => PrimitiveJCodecs.bigint
      case PBlob       => PrimitiveJCodecs.bytes
      case PBoolean    => PrimitiveJCodecs.boolean
      case PByte       => PrimitiveJCodecs.byte
      case PDocument   => documentJCodec
      case PDouble     => PrimitiveJCodecs.double
      case PFloat      => PrimitiveJCodecs.float
      case PInt        => PrimitiveJCodecs.int
      case PLong       => PrimitiveJCodecs.long
      case PShort      => PrimitiveJCodecs.short
      case PString     => PrimitiveJCodecs.string
      case PTimestamp =>
        val format =
          hints.get(TimestampFormat).getOrElse(TimestampFormat.DATE_TIME)
        format match {
          case TimestampFormat.DATE_TIME => PrimitiveJCodecs.timestampDateTime
          case TimestampFormat.EPOCH_SECONDS => PrimitiveJCodecs.timestampEpoch
          case TimestampFormat.HTTP_DATE => PrimitiveJCodecs.timestampHttpDate
        }
      case PUnit => PrimitiveJCodecs.unit
      case PUUID => PrimitiveJCodecs.uuid
    }
  }

  private def listImpl[A](member: Schema[A]) = new JCodec[List[A]] {
    private[this] val a: JCodec[A] = apply(member)
    def expecting: String = "list"

    override def canBeKey: Boolean = false

    def decodeValue(cursor: Cursor, in: JsonReader): List[A] =
      if (in.isNextToken('[')) {
        if (in.isNextToken(']')) Nil
        else {
          in.rollbackToken()
          val builder = new ListBuffer[A]
          var i = 0
          while ({
            if (i >= maxArity)
              throw cursor.payloadError(
                this,
                s"input $expecting exceeded max arity of `$maxArity`"
              )
            builder += cursor.under(i)(cursor.decode(a, in))
            i += 1
            in.isNextToken(',')
          }) ()
          if (in.isCurrentToken(']')) builder.result()
          else in.arrayEndOrCommaError()
        }
      } else in.decodeError("Expected JSON array")

    def encodeValue(xs: List[A], out: JsonWriter): Unit = {
      out.writeArrayStart()
      var list = xs
      while (list ne Nil) {
        a.encodeValue(list.head, out)
        list = list.tail
      }
      out.writeArrayEnd()
    }

    def decodeKey(in: JsonReader): List[A] =
      in.decodeError("Cannot use vectors as keys")

    def encodeKey(xs: List[A], out: JsonWriter): Unit =
      out.encodeError("Cannot use vectors as keys")
  }

  override def list[A](
      shapeId: ShapeId,
      hints: Hints,
      member: Schema[A]
  ): JCodec[List[A]] = listImpl(member)

  override def set[A](
      shapeId: ShapeId,
      hints: Hints,
      member: Schema[A]
  ): JCodec[Set[A]] = new JCodec[Set[A]] {
    private[this] val a = apply(member)
    def expecting: String = "list"

    override def canBeKey: Boolean = false

    def decodeValue(cursor: Cursor, in: JsonReader): Set[A] =
      if (in.isNextToken('[')) {
        if (in.isNextToken(']')) Set.empty
        else {
          in.rollbackToken()
          val builder = Set.newBuilder[A]
          var i = 0
          while ({
            if (i >= maxArity)
              throw cursor.payloadError(
                this,
                s"input $expecting exceeded max arity of `$maxArity`"
              )
            builder += cursor.under(i)(cursor.decode(a, in))
            i += 1
            in.isNextToken(',')
          }) ()
          if (in.isCurrentToken(']')) builder.result()
          else in.arrayEndOrCommaError()
        }
      } else in.decodeError("Expected JSON array")

    def encodeValue(xs: Set[A], out: JsonWriter): Unit = {
      out.writeArrayStart()
      xs.foreach(x => a.encodeValue(x, out))
      out.writeArrayEnd()
    }

    def decodeKey(in: JsonReader): Set[A] =
      in.decodeError("Cannot use vectors as keys")

    def encodeKey(xs: Set[A], out: JsonWriter): Unit =
      out.encodeError("Cannot use vectors as keys")
  }

  private def objectMap[K, V](
      jk: JCodec[K],
      jv: JCodec[V]
  ): JCodec[Map[K, V]] = {
    new JCodec[Map[K, V]] {
      val expecting: String = "map"

      override def canBeKey: Boolean = false

      def decodeValue(cursor: Cursor, in: JsonReader): Map[K, V] =
        if (in.isNextToken('{')) {
          if (in.isNextToken('}')) Map.empty
          else {
            in.rollbackToken()
            val builder = Map.newBuilder[K, V]
            var i = 0
            while ({
              if (i >= maxArity)
                throw cursor.payloadError(
                  this,
                  s"input $expecting exceeded max arity of `$maxArity`"
                )
              builder += (
                (
                  jk.decodeKey(in),
                  cursor.under(i)(cursor.decode(jv, in))
                )
              )
              i += 1
              in.isNextToken(',')
            }) ()
            if (in.isCurrentToken('}')) builder.result()
            else in.objectEndOrCommaError()
          }
        } else in.decodeError("Expected JSON object")

      def encodeValue(xs: Map[K, V], out: JsonWriter): Unit = {
        out.writeObjectStart()
        xs.foreach { case (key, value) =>
          jk.encodeKey(key, out)
          jv.encodeValue(value, out)
        }
        out.writeObjectEnd()
      }

      def decodeKey(in: JsonReader): Map[K, V] =
        in.decodeError("Cannot use maps as keys")

      def encodeKey(xs: Map[K, V], out: JsonWriter): Unit =
        out.encodeError("Cannot use maps as keys")
    }
  }

  private def arrayMap[K, V](
      k: Schema[K],
      v: Schema[V]
  ): JCodec[Map[K, V]] = {
    val kField = Field.required[Schema, (K, V), K]("key", k, _._1)
    val vField = Field.required[Schema, (K, V), V]("value", v, _._2)
    val kvCodec = Schema.struct(Vector(kField, vField))(vec =>
      (vec(0).asInstanceOf[K], vec(1).asInstanceOf[V])
    )
    listImpl(kvCodec).biject(_.toMap, _.toList)
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): JCodec[Map[K, V]] = {
    val jk = apply(key)
    val jv = apply(value)
    if (jk.canBeKey) objectMap(jk, jv)
    else arrayMap(key, value)
  }

  override def biject[A, B](
      schema: Schema[A],
      to: A => B,
      from: B => A
  ): JCodec[B] =
    apply(schema).biject(to, from)
  override def surject[A, B](
      schema: Schema[A],
      to: Refinement[A, B],
      from: B => A
  ): JCodec[B] =
    JCodec.jcodecInvariant
      .xmap(apply(schema))(to.asFunction, from)
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

  private def taggedUnion[Z](
      alternatives: Vector[Alt[Schema, Z, _]]
  )(total: Z => Alt.WithValue[Schema, Z, _]): JCodec[Z] =
    new JCodec[Z] {
      val expecting: String = "tagged-union"

      override def canBeKey: Boolean = false

      def jsonLabel[A](alt: Alt[Schema, Z, A]): String =
        alt.hints.get(JsonName).map(_.value).getOrElse(alt.label)

      private[this] val handlerMap
          : util.HashMap[String, (Cursor, JsonReader) => Z] =
        new util.HashMap[String, (Cursor, JsonReader) => Z] {
          def handler[A](alt: Alt[Schema, Z, A]) = {
            val codec = apply(alt.instance)
            (cursor: Cursor, reader: JsonReader) =>
              alt.inject(cursor.decode(codec, reader))
          }

          alternatives.foreach(alt => put(jsonLabel(alt), handler(alt)))
        }

      def decodeValue(cursor: Cursor, in: JsonReader): Z =
        if (in.isNextToken('{')) {
          if (in.isNextToken('}'))
            in.decodeError("Expected a single key/value pair")
          else {
            in.rollbackToken()
            val key = in.readKeyAsString()
            val result = cursor.under(key) {
              val handler = handlerMap.get(key)
              if (handler eq null) in.discriminatorValueError(key)
              handler(cursor, in)
            }
            if (in.isNextToken('}')) result
            else {
              in.rollbackToken()
              in.decodeError(s"Expected no other field after $key")
            }
          }
        } else in.decodeError("Expected JSON object")

      private[this] val altCache =
        new PolyFunction[Alt[Schema, Z, *], JCodec] {
          def apply[A](fa: Alt[Schema, Z, A]): JCodec[A] =
            self.apply(fa.instance)
        }.unsafeCache((alternatives).map(alt => Existential.wrap(alt)))

      def encodeValue(z: Z, out: JsonWriter): Unit = {
        def writeValue[A](awv: Alt.WithValue[Schema, Z, A]): Unit =
          altCache(awv.alt).encodeValue(awv.value, out)

        val awv = total(z)
        out.writeObjectStart()
        out.writeKey(jsonLabel(awv.alt))
        writeValue(awv)
        out.writeObjectEnd()
      }

      def decodeKey(in: JsonReader): Z =
        in.decodeError("Cannot use coproducts as keys")

      def encodeKey(x: Z, out: JsonWriter): Unit =
        out.encodeError("Cannot use coproducts as keys")
    }

  private def untaggedUnion[Z](
      alternatives: Vector[Alt[Schema, Z, _]]
  )(total: Z => Alt.WithValue[Schema, Z, _]): JCodec[Z] = new JCodec[Z] {
    def expecting: String = "untaggedUnion"

    override def canBeKey: Boolean = false

    private[this] val handlerList: Array[(Cursor, JsonReader) => Z] = {
      val res = Array.newBuilder[(Cursor, JsonReader) => Z]
      def handler[A](alt: Alt[Schema, Z, A]) = {
        val codec = apply(alt.instance)
        (cursor: Cursor, reader: JsonReader) =>
          alt.inject(cursor.decode(codec, reader))
      }
      alternatives.foreach(alt => res += handler(alt))
      res.result()
    }

    def decodeValue(cursor: Cursor, in: JsonReader): Z = {
      var z: Z = null.asInstanceOf[Z]
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

    private[this] val altCache =
      new PolyFunction[Alt[Schema, Z, *], JCodec] {
        def apply[A](fa: Alt[Schema, Z, A]): JCodec[A] = self.apply(fa.instance)
      }.unsafeCache((alternatives).map(alt => Existential.wrap(alt)))

    def encodeValue(z: Z, out: JsonWriter): Unit = {
      def writeValue[A](awv: Alt.WithValue[Schema, Z, A]): Unit =
        altCache(awv.alt).encodeValue(awv.value, out)

      val awv = total(z)
      writeValue(awv)
    }

    def decodeKey(in: JsonReader): Z =
      in.decodeError("Cannot use coproducts as keys")

    def encodeKey(x: Z, out: JsonWriter): Unit =
      out.encodeError("Cannot use coproducts as keys")
  }

  private def discriminatedUnion[Z](
      alternatives: Vector[Alt[Schema, Z, _]],
      discriminated: Discriminated
  )(total: Z => Alt.WithValue[Schema, Z, _]): JCodec[Z] =
    new JCodec[Z] {
      def expecting: String = "discriminated-union"

      override def canBeKey: Boolean = false

      def jsonLabel[A](alt: Alt[Schema, Z, A]): String =
        alt.hints.get(JsonName).map(_.value).getOrElse(alt.label)

      private[this] val handlerMap
          : util.HashMap[String, (Cursor, JsonReader) => Z] =
        new util.HashMap[String, (Cursor, JsonReader) => Z] {
          def handler[A](
              alt: Alt[Schema, Z, A]
          ): (Cursor, JsonReader) => Z = {
            val codec = apply(alt.instance)
            (cursor: Cursor, reader: JsonReader) =>
              alt.inject(cursor.decode(codec, reader))
          }

          alternatives.foreach(alt => put(jsonLabel(alt), handler(alt)))
        }

      def decodeValue(cursor: Cursor, in: JsonReader): Z =
        if (in.isNextToken('{')) {
          in.setMark()
          if (in.skipToKey(discriminated.value)) {
            val key = in.readString("")
            in.rollbackToMark()
            in.rollbackToken()
            cursor.under(key) {
              val handler = handlerMap.get(key)
              if (handler eq null) in.discriminatorValueError(key)
              handler(cursor, in)
            }
          } else
            in.decodeError(
              s"Unable to find discriminator ${discriminated.value}"
            )
        } else in.decodeError("Expected JSON object")

      private[this] val altCache =
        new PolyFunction[Alt[Schema, Z, *], JCodec] {
          def apply[A](fa: Alt[Schema, Z, A]): JCodec[A] = {
            val label = jsonLabel(fa)
            self.apply(
              fa.instance
                .addHints(
                  Hints(DiscriminatedUnionMember(discriminated.value, label))
                )
            )
          }
        }.unsafeCache((alternatives).map(alt => Existential.wrap(alt)))

      def encodeValue(z: Z, out: JsonWriter): Unit = {
        def writeValue[A](awv: Alt.WithValue[Schema, Z, A]): Unit =
          altCache(awv.alt).encodeValue(awv.value, out)

        val awv = total(z)
        writeValue(awv)
      }

      def decodeKey(in: JsonReader): Z =
        in.decodeError("Cannot use coproducts as keys")

      def encodeKey(x: Z, out: JsonWriter): Unit =
        out.encodeError("Cannot use coproducts as keys")
    }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: U => Alt.SchemaAndValue[U, _]
  ): JCodec[U] = hints match {
    case Untagged.hint(_)      => untaggedUnion(alternatives)(dispatch)
    case Discriminated.hint(d) => discriminatedUnion(alternatives, d)(dispatch)
    case _                     => taggedUnion(alternatives)(dispatch)
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): JCodec[E] = new JCodec[E] {
    def fromName(v: String): Option[E] =
      values.find(_.stringValue == v).map(_.value)
    val expecting: String =
      s"enumeration: [${values.map(_.stringValue).mkString(", ")}]"

    def decodeValue(cursor: Cursor, in: JsonReader): E = {
      val str = in.readString(null)
      fromName(str) match {
        case Some(value) => value
        case None        => in.enumValueError(str)
      }
    }

    def encodeValue(x: E, out: JsonWriter): Unit =
      out.writeVal(total(x).stringValue)

    def decodeKey(in: JsonReader): E = {
      val str = in.readKeyAsString()
      fromName(str) match {
        case Some(value) => value
        case None        => in.enumValueError(str)
      }
    }

    def encodeKey(x: E, out: JsonWriter): Unit =
      out.writeKey(total(x).stringValue)
  }

  private def jsonLabel[A, Z](field: Field[Schema, Z, A]): String =
    field.hints
      .get(JsonName)
      .map(_.value)
      .getOrElse(field.label)

  private type Handler = (Cursor, JsonReader, util.HashMap[String, Any]) => Unit

  private def fieldHandler[Z, A](
      field: Field[Schema, Z, A]
  ): Handler = {
    val codec = apply(field.instance)
    val label = field.label
    if (field.isRequired) { (cursor, in, mmap) =>
      val _ = mmap.put(label, cursor.under(label)(cursor.decode(codec, in)))
    } else { (cursor, in, mmap) =>
      cursor.under[Unit](label) {
        if (in.isNextToken('n')) in.readNullOrError[Unit]((), "Expected null")
        else {
          in.rollbackToken()
          val _ = mmap.put(label, cursor.decode(codec, in))
        }
      }
    }
  }

  private def fieldEncoder[Z, A](
      field: Field[Schema, Z, A]
  ): (Z, JsonWriter) => Unit = {
    field.fold(new Field.Folder[Schema, Z, (Z, JsonWriter) => Unit] {
      def onRequired[AA](
          label: String,
          instance: Schema[AA],
          get: Z => AA
      ): (Z, JsonWriter) => Unit = {
        val codec = apply(instance)
        val jLabel = jsonLabel(field)
        if (jLabel.forall(JsonWriter.isNonEscapedAscii)) {
          (z: Z, out: JsonWriter) =>
            {
              out.writeNonEscapedAsciiKey(jLabel)
              codec.encodeValue(get(z), out)
            }
        } else { (z: Z, out: JsonWriter) =>
          {
            out.writeKey(jLabel)
            codec.encodeValue(get(z), out)
          }
        }
      }

      def onOptional[AA](
          label: String,
          instance: Schema[AA],
          get: Z => Option[AA]
      ): (Z, JsonWriter) => Unit = {
        val codec = apply(instance)
        val jLabel = jsonLabel(field)
        if (jLabel.forall(JsonWriter.isNonEscapedAscii)) {
          (z: Z, out: JsonWriter) =>
            {
              get(z) match {
                case Some(aa) =>
                  out.writeNonEscapedAsciiKey(jLabel)
                  codec.encodeValue(aa, out)
                case _ =>
              }
            }
        } else { (z: Z, out: JsonWriter) =>
          {
            get(z) match {
              case Some(aa) =>
                out.writeKey(jLabel)
                codec.encodeValue(aa, out)
              case _ =>
            }
          }
        }
      }
    })
  }

  private type Fields[Z] = Vector[Field[Schema, Z, _]]

  private def nonPayloadStruct[Z](
      fields: Fields[Z],
      maybeInputOutput: Option[InputOutput]
  )(
      const: Vector[Any] => Z,
      encode: (Z, JsonWriter, Vector[(Z, JsonWriter) => Unit]) => Unit
  ): JCodec[Z] =
    new JCodec[Z] {

      private[this] val documentFields =
        fields.filter { field =>
          HttpBinding
            .fromHints(field.label, field.hints, maybeInputOutput)
            .isEmpty
        }

      private[this] val handlers =
        new util.HashMap[String, Handler](documentFields.length) {
          documentFields.foreach(field =>
            put(jsonLabel(field), fieldHandler(field))
          )
        }

      private[this] val documentEncoders =
        documentFields.map(field => fieldEncoder(field))

      def expecting: String = "object"

      override def canBeKey = false

      def decodeValue(cursor: Cursor, in: JsonReader): Z =
        decodeValue_(cursor, in)(emptyMetadata)

      override def decodeMessage(
          in: JsonReader
      ): scala.collection.Map[String, Any] => Z =
        Cursor.withCursor(expecting)(decodeValue_(_, in))

      private def decodeValue_(
          cursor: Cursor,
          in: JsonReader
      ): scala.collection.Map[String, Any] => Z = {
        val buffer = new util.HashMap[String, Any](handlers.size)
        if (in.isNextToken('{')) {
          // In this case, metadata and payload are mixed together
          // and values field values must be sought from either.
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
        // We therefore reconcile the values pulled from the json
        // with the ones pull the metadata, and call the constructor
        // on it.
        { (meta: scala.collection.Map[String, Any]) =>
          meta.foreach(kv => buffer.put(kv._1, kv._2))
          val stage2 = new VectorBuilder[Any]
          fields.foreach(f =>
            stage2 += {
              val value = buffer.get(f.label)
              if (f.isRequired) {
                if (value == null) cursor.requiredFieldError(f.label, f.label)
                value
              } else Option(value)
            }
          )
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

  private def payloadStruct[A, Z](
      payloadField: Field[Schema, Z, _],
      fields: Fields[Z]
  )(codec: JCodec[payloadField.T], const: Vector[Any] => Z): JCodec[Z] =
    new JCodec[Z] {
      def expecting: String = "object"

      override def canBeKey = false

      def decodeValue(cursor: Cursor, in: JsonReader): Z =
        decodeValue_(cursor, in)(emptyMetadata)

      override def decodeMessage(
          in: JsonReader
      ): scala.collection.Map[String, Any] => Z =
        Cursor.withCursor(expecting)(decodeValue_(_, in))

      private def decodeValue_(
          cursor: Cursor,
          in: JsonReader
      ): scala.collection.Map[String, Any] => Z = {
        val buffer = new util.HashMap[String, Any](1)
        // In this case, one field assumes the whole payload. We use
        // its associated codec.
        buffer.put(payloadField.label, cursor.decode(codec, in))

        // At this point, we have parsed the json and retrieved
        // all the values that interest us for the construction
        // of our domain object.
        // We therefore reconcile the values pulled from the json
        // with the ones pull the metadata, and call the constructor
        // on it.
        { (meta: scala.collection.Map[String, Any]) =>
          meta.foreach(kv => buffer.put(kv._1, kv._2))
          val stage2 = new VectorBuilder[Any]
          fields.foreach(f =>
            stage2 += {
              val value = buffer.get(f.label)
              if (f.isRequired) {
                if (value == null) cursor.requiredFieldError(f.label, f.label)
                value
              } else Option(value)
            }
          )
          const(stage2.result())
        }
      }

      def encodeValue(z: Z, out: JsonWriter): Unit =
        payloadField.foreachT(z)(codec.encodeValue(_, out))

      def decodeKey(in: JsonReader): Z =
        in.decodeError("Cannot use products as keys")

      def encodeKey(x: Z, out: JsonWriter): Unit =
        out.encodeError("Cannot use products as keys")
    }

  private def basicStruct[A, S](
      fields: Fields[S],
      maybeInputOutput: Option[InputOutput]
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

    nonPayloadStruct(fields, maybeInputOutput)(make, encode)
  }
  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): JCodec[S] = {
    (
      InputOutput.hint.unapply(hints),
      DiscriminatedUnionMember.hint.unapply(hints)
    ) match {
      case (maybeInputOutput, maybeDiscriminated) =>
        fields.find(_.hints.get(HttpPayload).isDefined) match {
          case Some(payloadField) =>
            val codec = apply(payloadField.instance)
            payloadStruct(payloadField, fields)(codec, make)
          case None =>
            maybeDiscriminated match {
              case Some(d) =>
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
                nonPayloadStruct(fields, maybeInputOutput)(make, encode)
              case None =>
                basicStruct(fields, maybeInputOutput)(make)
            }
        }
    }
  }

}
