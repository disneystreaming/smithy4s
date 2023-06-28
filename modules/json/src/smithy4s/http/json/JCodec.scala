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

import com.github.plokhotnyuk.jsoniter_scala.core._
import smithy4s.capability.Invariant

import scala.collection.{Map => MMap}
import smithy4s.schema.CachedSchemaCompiler

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

object JCodec extends CachedSchemaCompiler.Impl[JCodec] {

  implicit val jcodecInvariant: Invariant[JCodec] = new Invariant[JCodec] {
    def imap[A, B](fa: JCodec[A])(to: A => B, from: B => A): JCodec[B] =
      fa.biject(to, from)

    def xmap[A, B](fa: JCodec[A])(
        to: A => Either[smithy4s.ConstraintError, B],
        from: B => A
    ): JCodec[B] = {
      val throwingTo: A => B = to(_) match {
        case Left(error)  => throw error
        case Right(value) => value
      }
      imap(fa)(throwingTo, from)
    }
  }

  type Aux[A] = JCodec[A]

  def fromSchema[A](
      schema: Schema[A],
      cache: Cache
  ): JCodec[A] = schema.compile(codecs.schemaVisitorJCodec(cache))

}
