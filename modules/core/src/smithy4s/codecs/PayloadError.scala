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

package smithy4s.codecs

import smithy4s.schema.Schema._
import smithy4s.schema._

case class PayloadError private (
    path: PayloadPath,
    expected: String,
    message: String
) extends Throwable
    with scala.util.control.NoStackTrace {
  def withPath(value: PayloadPath): PayloadError = {
    copy(path = value)
  }

  def withExpected(value: String): PayloadError = {
    copy(expected = value)
  }

  def withMessage(value: String): PayloadError = {
    copy(message = value)
  }
  override def toString(): String =
    s"PayloadError($path, expected = $expected, message=$message)"
  override def getMessage(): String = s"$message (path: $path)"
}

object PayloadError {
  @scala.annotation.nowarn(
    "msg=private method unapply in object PayloadError is never used"
  )
  private def unapply(c: PayloadError): Option[PayloadError] = Some(c)
  def apply(
      path: PayloadPath,
      expected: String,
      message: String
  ): PayloadError = {
    new PayloadError(path, expected, message)
  }

  val schema: Schema[PayloadError] = {
    val path = PayloadPath.schema.required[PayloadError]("path", _.path)
    val expected = string.required[PayloadError]("expected", _.expected)
    val message = string.required[PayloadError]("message", _.message)
    struct(path, expected, message)(PayloadError.apply)
  }
}
