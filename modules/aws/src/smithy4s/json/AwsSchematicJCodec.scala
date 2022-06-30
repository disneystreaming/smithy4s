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
import smithy4s.schema.Primitive

private[aws] object AwsSchematicJCodec
    extends SchemaVisitorJCodec(maxArity = 1024) {

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): JCodec[P] = {
    tag match {
      case Primitive.PTimestamp => timestamp
      case _                    => super.primitive(shapeId, hints, tag)
    }
  }

  private val timestamp: JCodec[Timestamp] = new JCodec[Timestamp] {
    val expecting: String = "instant (epoch second)"

    def decodeValue(cursor: Cursor, in: JsonReader): Timestamp = Timestamp(in.readDouble().toLong, 0)

    def encodeValue(x: Timestamp, out: JsonWriter): Unit = out.writeVal(x.epochSecond)

    def decodeKey(in: JsonReader): Timestamp = Timestamp(in.readKeyAsDouble().toLong, 0)

    def encodeKey(x: Timestamp, out: JsonWriter): Unit = out.writeKey(x.epochSecond)
  }

}
