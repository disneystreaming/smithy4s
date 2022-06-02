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
import smithy4s.http.json.SchematicJCodec
import smithy4s.internals.Hinted

private[aws] object AwsSchematicJCodec
    extends SchematicJCodec(maxArity = 1024) {

  override def timestamp: JCodec.JCodecMake[Timestamp] = Hinted.static {
    new JCodec[Timestamp] {
      val expecting: String = "instant (epoch second)"

      def decodeValue(cursor: Cursor, in: JsonReader): Timestamp =
        Timestamp.fromEpochSecond(in.readDouble().toLong)

      def encodeValue(x: Timestamp, out: JsonWriter): Unit =
        out.writeVal(x.epochSecond)

      def decodeKey(in: JsonReader): Timestamp =
        Timestamp.fromEpochSecond(in.readKeyAsDouble().toLong)

      def encodeKey(x: Timestamp, out: JsonWriter): Unit =
        out.writeKey(x.epochSecond)
    }
  }

}
