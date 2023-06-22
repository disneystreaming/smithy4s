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
package aws.json

import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader
import com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter
import smithy4s.Timestamp
import smithy4s.http.json.Cursor
import smithy4s.http.json.JCodec
import smithy4s.http.json.SchemaVisitorJCodec
import smithy4s.schema.CompilationCache
import smithy4s.schema.Primitive

private[aws] class AwsSchemaVisitorJCodec(cache: CompilationCache[JCodec])
    extends SchemaVisitorJCodec(
      maxArity = 1024,
      explicitNullEncoding = false,
      cache
    ) {

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): JCodec[P] = {
    tag match {
      case Primitive.PTimestamp => timestamp
      case Primitive.PDouble    => double
      case Primitive.PFloat     => float
      case _                    => super.primitive(shapeId, hints, tag)
    }
  }

  private val double: JCodec[Double] = new JCodec[Double] {
    val expecting: String = "JSON number for numeric values"

    def decodeValue(cursor: Cursor, in: JsonReader): Double =
      if (in.isNextToken('"')) {
        in.rollbackToken()
        val len = in.readStringAsCharBuf()
        if (in.isCharBufEqualsTo(len, "NaN")) Double.NaN
        else if (in.isCharBufEqualsTo(len, "Infinity")) Double.PositiveInfinity
        else if (in.isCharBufEqualsTo(len, "-Infinity")) Double.NegativeInfinity
        else in.decodeError("illegal double")
      } else {
        in.rollbackToken()
        in.readDouble()
      }

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

  private val float: JCodec[Float] = new JCodec[Float] {
    val expecting: String = "JSON number for numeric values"

    def decodeValue(cursor: Cursor, in: JsonReader): Float =
      if (in.isNextToken('"')) {
        in.rollbackToken()
        val len = in.readStringAsCharBuf()
        if (in.isCharBufEqualsTo(len, "NaN")) Float.NaN
        else if (in.isCharBufEqualsTo(len, "Infinity")) Float.PositiveInfinity
        else if (in.isCharBufEqualsTo(len, "-Infinity")) Float.NegativeInfinity
        else in.decodeError("illegal float")
      } else {
        in.rollbackToken()
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

  private val timestamp: JCodec[Timestamp] = new JCodec[Timestamp] {
    val expecting: String = "instant (epoch second)"

    def decodeValue(cursor: Cursor, in: JsonReader): Timestamp =
      Timestamp(in.readDouble().toLong, 0)

    def encodeValue(x: Timestamp, out: JsonWriter): Unit =
      out.writeVal(x.epochSecond)

    def decodeKey(in: JsonReader): Timestamp =
      Timestamp(in.readKeyAsDouble().toLong, 0)

    def encodeKey(x: Timestamp, out: JsonWriter): Unit =
      out.writeKey(x.epochSecond)
  }

}
