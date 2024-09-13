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

import scala.annotation.nowarn

case class RawErrorResponse private (
    code: Int,
    headers: Map[CaseInsensitive, Seq[String]],
    body: String,
    failedDecodeAttempt: FailedDecodeAttempt
) extends Throwable {

  def withCode(code: Int): RawErrorResponse =
    copy(code = code)

  def withHeaders(
      headers: Map[CaseInsensitive, Seq[String]]
  ): RawErrorResponse =
    copy(headers = headers)

  def withBody(body: String): RawErrorResponse =
    copy(body = body)

  def withFailedDecodeAttempt(
      failedDecodeAttempt: FailedDecodeAttempt
  ): RawErrorResponse =
    copy(failedDecodeAttempt = failedDecodeAttempt)

  override def getMessage(): String = {
    val baseMessage = s"status $code, headers: $headers, body:\n$body"
    baseMessage + s"""
                     |FailedDecodeAttempt:
                     |  ${failedDecodeAttempt.getMessage}
       """.stripMargin
  }

  override def getCause: Throwable = failedDecodeAttempt
}

object RawErrorResponse {
  def apply(
      code: Int,
      headers: Map[CaseInsensitive, Seq[String]],
      body: String,
      failedDecodeAttempt: FailedDecodeAttempt
  ): RawErrorResponse =
    new RawErrorResponse(code, headers, body, failedDecodeAttempt)

  @nowarn
  private def unapply(response: RawErrorResponse): Option[
    (Int, Map[CaseInsensitive, Seq[String]], String, FailedDecodeAttempt)
  ] =
    Some(
      (
        response.code,
        response.headers,
        response.body,
        response.failedDecodeAttempt
      )
    )

}

sealed trait FailedDecodeAttempt extends Throwable {
  def discriminator: HttpDiscriminator
  def getMessage: String
}

object FailedDecodeAttempt {

  case class UnrecognisedDiscriminator(discriminator: HttpDiscriminator)
      extends FailedDecodeAttempt {
    override def getMessage: String =
      s"Unrecognised discriminator: $discriminator"
  }

  case class DecodingFailure(
      discriminator: HttpDiscriminator,
      contractError: HttpContractError
  ) extends FailedDecodeAttempt {
    override def getMessage: String =
      s"Decoding failed for discriminator: $discriminator with error: ${contractError.getMessage}"
  }
}
