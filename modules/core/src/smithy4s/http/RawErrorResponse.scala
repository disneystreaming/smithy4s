package smithy4s.http

case class RawErrorResponse(
    code: Int,
    headers: Map[CaseInsensitive, Seq[String]],
    body: String,
    failedDecodeAttempt: Option[FailedDecodeAttempt]
) extends Throwable {
  override def getMessage(): String = {
    val baseMessage = s"status $code, headers: $headers, body:\n$body"
    failedDecodeAttempt match {
      case Some(attempt) =>
        baseMessage +
          s"""
             |FailedDecodeAttempt:
             |  discriminator: ${attempt.discriminator}
             |  contractError: ${attempt.contractError.getMessage}
               """.stripMargin
      case None => baseMessage
    }
  }
}

case class FailedDecodeAttempt(
    discriminator: HttpDiscriminator,
    contractError: HttpContractError
)
