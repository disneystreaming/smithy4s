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
package http.json

import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader
import com.github.plokhotnyuk.jsoniter_scala.core.JsonReaderException
import smithy4s.http.PayloadError

class Cursor private () {
  private[this] var indexStack: Array[Int] = new Array[Int](8)
  private[this] var labelStack: Array[String] = new Array[String](8)
  private[this] var top: Int = _
  private var expecting: String = _

  def decode[A](codec: JCodec[A], in: JsonReader): A = {
    this.expecting = codec.expecting
    codec.decodeValue(this, in)
  }

  def under[A](label: String)(f: => A): A = {
    if (top >= labelStack.length) growStacks()
    labelStack(top) = label
    top += 1
    val res = f
    top -= 1
    res
  }

  def under[A](index: Int)(f: => A): A = {
    if (top >= indexStack.length) growStacks()
    indexStack(top) = index
    top += 1
    val res = f
    top -= 1
    res
  }

  def payloadError[A](codec: JCodec[A], message: String): Nothing =
    throw new PayloadError(getPath(Nil), codec.expecting, message)

  def requiredFieldError[A](codec: JCodec[A], field: String): Nothing =
    requiredFieldError(codec.expecting, field)

  def requiredFieldError[A](expecting: String, field: String): Nothing = {
    val path = getPath(new PayloadPath.Segment.Label(field) :: Nil)
    throw new PayloadError(path, expecting, "Missing required field")
  }

  private def getPath(segments: List[PayloadPath.Segment]): PayloadPath = {
    var top = this.top
    var list = segments
    while (top > 0) {
      top -= 1
      val label = labelStack(top)
      val segment =
        if (label ne null) new PayloadPath.Segment.Label(label)
        else new PayloadPath.Segment.Index(indexStack(top))
      list = segment :: list
    }
    new PayloadPath(list)
  }

  private def getExpected(): String =
    if (expecting != null) expecting
    else throw new IllegalStateException("Expected should have been fulfilled")

  private[this] def growStacks(): Unit = {
    val size = top << 1
    labelStack = java.util.Arrays.copyOf(labelStack, size)
    indexStack = java.util.Arrays.copyOf(indexStack, size)
  }
}

object Cursor {

  def withCursor[A](expecting: String)(f: Cursor => A): A = {
    val cursor = new Cursor()
    cursor.expecting = expecting
    try f(cursor)
    catch {
      case e: JsonReaderException => payloadError(cursor, e.getMessage())
      case e: ConstraintError     => payloadError(cursor, e.message)
    }
  }

  private[this] def payloadError(cursor: Cursor, message: String): Nothing =
    throw new PayloadError(cursor.getPath(Nil), cursor.getExpected(), message)
}
