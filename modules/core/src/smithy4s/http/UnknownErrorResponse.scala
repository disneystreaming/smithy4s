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

package smithy4s.http

case class UnknownErrorResponse private (
    code: Int,
    headers: Map[CaseInsensitive, Seq[String]],
    body: String
) extends Throwable {
  def withCode(value: Int): UnknownErrorResponse = {
    copy(code = value)
  }

  def withHeaders(
      value: Map[CaseInsensitive, Seq[String]]
  ): UnknownErrorResponse = {
    copy(headers = value)
  }

  def withBody(value: String): UnknownErrorResponse = {
    copy(body = value)
  }
  override def getMessage(): String =
    s"status $code, headers: $headers, body:\n$body"
}

object UnknownErrorResponse {
  @scala.annotation.nowarn(
    "msg=private method unapply in object UnknownErrorResponse is never used"
  )
  private def unapply(c: UnknownErrorResponse): Option[UnknownErrorResponse] =
    Some(c)
  def apply(
      code: Int,
      headers: Map[CaseInsensitive, Seq[String]],
      body: String
  ): UnknownErrorResponse = {
    new UnknownErrorResponse(code, headers, body)
  }
}
