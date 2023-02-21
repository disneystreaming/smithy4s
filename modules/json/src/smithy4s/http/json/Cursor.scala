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

import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader
import com.github.plokhotnyuk.jsoniter_scala.core.JsonReaderException
import smithy4s.http.PayloadError

class Cursor private () {
  private[this] var stack: Array[PayloadPath.Segment] =
    new Array[PayloadPath.Segment](8)
  private[this] var top: Int = 0
  private var expecting: String = null

  def decode[A](codec: JCodec[A], in: JsonReader): A = {
    this.expecting = codec.expecting
    codec.decodeValue(this, in)
  }

  def under[A](segment: PayloadPath.Segment)(f: => A): A = {
    if (top >= stack.length) stack = java.util.Arrays.copyOf(stack, top << 1)
    stack(top) = segment
    top += 1
    val res = f
    top -= 1
    res
  }

  def under[A](label: String)(f: => A): A =
    under(new PayloadPath.Segment.Label(label))(f)

  def under[A](index: Int)(f: => A): A =
    under(new PayloadPath.Segment.Index(index))(f)

  def payloadError[A](codec: JCodec[A], message: String): Nothing =
    throw PayloadError(getPath(), codec.expecting, message)

  def requiredFieldError[A](codec: JCodec[A], field: String): Nothing =
    requiredFieldError(codec.expecting, field)

  def requiredFieldError[A](expecting: String, field: String): Nothing = {
    var top = this.top
    if (top >= stack.length) stack = java.util.Arrays.copyOf(stack, top << 1)
    stack(top) = new PayloadPath.Segment.Label(field)
    top += 1
    var list: List[PayloadPath.Segment] = Nil
    while (top > 0) {
      top -= 1
      list = stack(top) :: list
    }
    throw PayloadError(PayloadPath(list), expecting, "Missing required field")
  }

  private def getPath(): PayloadPath = {
    var top = this.top
    var list: List[PayloadPath.Segment] = Nil
    while (top > 0) {
      top -= 1
      list = stack(top) :: list
    }
    PayloadPath(list)
  }

  private def getExpected(): String =
    if (expecting != null) expecting
    else throw new IllegalStateException("Expected should have been fulfilled")
}

object Cursor {

  def withCursor[A](expecting: String)(f: Cursor => A): A = {
    val cursor = new Cursor()
    cursor.expecting = expecting
    try {
      f(cursor)
    } catch {
      case e: JsonReaderException => payloadError(cursor, e.getMessage())
      case e: ConstraintError     => payloadError(cursor, e.message)
    }
  }

  private[this] def payloadError(cursor: Cursor, message: String): Nothing =
    throw PayloadError(cursor.getPath(), cursor.getExpected(), message)
}
