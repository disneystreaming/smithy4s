/*
 *  Copyright 2021 Disney Streaming
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

import com.github.plokhotnyuk.jsoniter_scala.core._
import schematic.PolyFunction
import smithy4s.capability.Invariant
import smithy4s.internals.Hinted

import scala.collection.{Map => MMap}

/**
  * Construct that expresses the ability to decode an http message,
  * the metadata of which will have already been decoded and staged
  * in a Map[String, Any] indexed by field.
  *
  * On the encoding side, the fields that should be stored in metadata
  * are eluded.
  */
trait JCodec[A] extends JsonCodec[A] {
  self =>

  def canBeKey: Boolean = true

  def expecting: String

  /**
    * States whether this codec expects data
    * from the body of an http request (as opposed to
    * from headers, query params, etc). Used to prevent
    * parsing altogether when not required.
    */
  def expectBody: Boolean = true

  final val messageCodec: MessageCodec[A] =
    new MessageCodec[A] {

      def decodeValue(
          in: JsonReader,
          default: MMap[String, Any] => A
      ): MMap[String, Any] => A = self.decodeMessage(in)

      def encodeValue(x: MMap[String, Any] => A, out: JsonWriter): Unit =
        out.encodeError("Cannot encode as message codec")

      def nullValue: MMap[String, Any] => A =
        null.asInstanceOf[MMap[String, Any] => A]
    }

  def decodeMessage(in: JsonReader): MMap[String, Any] => A =
    Cursor.withCursor(expecting) { cursor =>
      val result = decodeValue(cursor, in)
      (_: MMap[String, Any]) => result
    }
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

      override def decodeMessage(in: JsonReader): MMap[String, Any] => B =
        self.decodeMessage(in) andThen to

      def decodeValue(cursor: Cursor, in: JsonReader): B =
        to(self.decodeValue(cursor, in))

      def decodeKey(in: JsonReader): B =
        to(self.decodeKey(in))

      def encodeValue(value: B, out: JsonWriter): Unit =
        self.encodeValue(from(value), out)

      def encodeKey(value: B, out: JsonWriter): Unit =
        self.encodeKey(from(value), out)

    }

  def widen[B >: A]: JCodec[B] = this.asInstanceOf[JCodec[B]]

}

object JCodec {

  implicit val jcodecInvariant: Invariant[JCodec] = new Invariant[JCodec] {
    def imap[A, B](fa: JCodec[A])(to: A => B, from: B => A): JCodec[B] =
      fa.biject(to, from)
  }

  type JCodecMake[A] = Hinted[JCodec, A]

  def fromSchema[A](schema: Schema[A]): JCodec[A] =
    schema.compile(codecs.schematicJCodec).get

  implicit def deriveJCodecFromSchema[A](implicit
      schema: Static[Schema[A]]
  ): JCodec[A] = jcodecCache(schema)

  private val jcodecCache =
    new PolyFunction[smithy4s.Schema, JCodec] {
      def apply[A](fa: smithy4s.Schema[A]): JCodec[A] = fromSchema(fa)
    }.unsafeMemoise

}
