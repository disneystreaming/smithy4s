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
package http.json

import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader
import com.github.plokhotnyuk.jsoniter_scala.core.JsonReaderException
import smithy4s.http.PayloadError

class Cursor private () {

  private var expecting: String = null
  private var stack: List[PayloadPath.Segment] = Nil

  def decode[A](codec: JCodec[A], in: JsonReader): A = {
    this.expecting = codec.expecting
    codec.decodeValue(this, in)
  }

  def under[A](segment: PayloadPath.Segment)(f: => A): A = {
    val prev = stack
    stack = segment :: stack
    val res = f
    stack = prev
    res
  }

  def under[A](label: String)(f: => A): A =
    under(PayloadPath.Segment.Label(label))(f)
  def under[A](index: Int)(f: => A): A =
    under(PayloadPath.Segment.Index(index))(f)

  def payloadError[A](codec: JCodec[A], message: String): Nothing = {
    val path = PayloadPath(stack.reverse)
    throw PayloadError(path, codec.expecting, message)
  }

  def requiredFieldError[A](codec: JCodec[A], field: String): Nothing = {
    requiredFieldError(codec.expecting, field)
  }

  def requiredFieldError[A](expecting: String, field: String): Nothing = {
    val path = PayloadPath((PayloadPath.Segment.Label(field) :: stack).reverse)
    throw PayloadError(path, expecting, "Missing required field")
  }

  private def getPath(): PayloadPath = PayloadPath(stack.reverse)

  private def getExpected() =
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
      case e: JsonReaderException =>
        throw PayloadError(
          cursor.getPath(),
          cursor.getExpected(),
          e.getMessage()
        )
      case Constraints.ConstraintError(_, message) =>
        throw PayloadError(
          cursor.getPath(),
          cursor.getExpected(),
          message
        )
    }
  }

}
