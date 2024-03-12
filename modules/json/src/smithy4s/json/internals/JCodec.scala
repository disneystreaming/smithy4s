/*
 *  Copyright 2021-2024 Disney Streaming
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

import com.github.plokhotnyuk.jsoniter_scala.core._

private[internals] trait JCodec[A] extends JsonCodec[A] {
  self =>

  def canBeKey: Boolean = true

  def expecting: String

  def decodeValue(cursor: Cursor, in: JsonReader): A

  override final def decodeValue(in: JsonReader, default: A): A =
    Cursor.withCursor(expecting) { cursor =>
      decodeValue(cursor, in)
    }

  override final def nullValue: A = null.asInstanceOf[A]

  def biject[B](to: A => B, from: B => A): JCodec[B] =
    new JCodec[B] {
      override def expecting: String = self.expecting
      override def canBeKey: Boolean = self.canBeKey

      def decodeValue(cursor: Cursor, in: JsonReader): B =
        to(self.decodeValue(cursor, in))

      def decodeKey(in: JsonReader): B =
        to(self.decodeKey(in))

      def encodeValue(value: B, out: JsonWriter): Unit =
        self.encodeValue(from(value), out)

      def encodeKey(value: B, out: JsonWriter): Unit =
        self.encodeKey(from(value), out)
    }

}
