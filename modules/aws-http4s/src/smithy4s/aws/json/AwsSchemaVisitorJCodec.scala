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
import smithy.api.TimestampFormat
import smithy4s.http.json.Cursor
import smithy4s.http.json.JCodec
import smithy4s.http.json.SchemaVisitorJCodec
import smithy4s.schema.CompilationCache
import smithy4s.schema.Primitive

private[aws] class AwsSchemaVisitorJCodec(
    cache: CompilationCache[JCodec],
    maxArity: Int = 1024
) extends SchemaVisitorJCodec(
      maxArity = maxArity,
      explicitNullEncoding = false,
      cache
    ) { self =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): JCodec[P] = {
    tag match {
      case Primitive.PTimestamp =>
        super.timestampJCodec(hints, TimestampFormat.EPOCH_SECONDS)
      case Primitive.PDouble => double
      case Primitive.PFloat  => float
      case _                 => super.primitive(shapeId, hints, tag)
    }
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): JCodec[Map[K, V]] = {
    value match {
      case _: Schema.NullableSchema[_] => super.map(shapeId, hints, key, value)
      case _ => flexibleNullParsingMap(shapeId, hints, key, value)
    }
  }

  // Overriding to be more flexible with null values : AWS
  // expects clients to be able to process and discard null values
  // in maps.
  private def flexibleNullParsingMap[K, V](
      shapeId: ShapeId,
      hints: Hints,
      sk: Schema[K],
      sv: Schema[V]
  ): JCodec[Map[K, V]] =
    new JCodec[Map[K, V]] {
      val expecting: String = "map"
      val jk = self(sk)
      val jv = self(sv)

      override def canBeKey: Boolean = false

      def decodeValue(cursor: Cursor, in: JsonReader): Map[K, V] =
        if (in.isNextToken('{')) {
          if (in.isNextToken('}')) Map.empty
          else {
            in.rollbackToken()
            val builder = Map.newBuilder[K, V]
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
                builder += (key -> value)
              }
              cursor.pop()

              i += 1
              in.isNextToken(',')
            }) ()
            if (in.isCurrentToken('}')) builder.result()
            else in.objectEndOrCommaError()
          }
        } else in.decodeError("Expected JSON object")

      def encodeValue(xs: Map[K, V], out: JsonWriter): Unit = {
        out.writeObjectStart()
        xs.foreach { kv =>
          jk.encodeKey(kv._1, out)
          jv.encodeValue(kv._2, out)
        }
        out.writeObjectEnd()
      }

      def decodeKey(in: JsonReader): Map[K, V] =
        in.decodeError("Cannot use maps as keys")

      def encodeKey(xs: Map[K, V], out: JsonWriter): Unit =
        out.encodeError("Cannot use maps as keys")

      private[this] def maxArityError(cursor: Cursor): Nothing =
        throw cursor.payloadError(
          this,
          s"Input $expecting exceeded max arity of $maxArity"
        )
    }

}
