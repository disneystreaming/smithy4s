package smithy4s.grpc

import alloy.proto.StatusDetails

sealed trait GrpcError extends Throwable with scala.util.control.NoStackTrace

object GrpcError {

  case class UnknownError(
    code: StatusCode,
    message: Option[String],
    details: StatusDetails
  ) extends GrpcError {
    override def getMessage(): String = {
      val detailsString =
        details
          .value
          .map { detailsEntry =>
            s"{type=${detailsEntry.typeUrl}, errorPayloadAsBase64=${detailsEntry.bytes.toBase64String}}"
          }
          .mkString("[", ", ", "]")
      s"Status: ${code.value}, message: ${message.getOrElse("")}, details: $detailsString"
    }
  }

  case class StatusDetailsDecodingFailed(
    code: StatusCode,
    message: Option[String],
    rawHeaderValue: String,
    reason: DecodingFailureReason
  ) extends GrpcError {
    override def getMessage(): String =
      s"Failed to decode grpc-status-details-bin header: ${reason.description}. " +
      s"Status: ${code.value}, message: ${message.getOrElse("")}, " +
      s"raw header value: $rawHeaderValue"
  }

  sealed trait DecodingFailureReason {
    def description: String
  }

  object DecodingFailureReason {
    case object Base64DecodingFailed extends DecodingFailureReason {
      def description: String = "base64 decoding failed"
    }
    case object ProtobufDecodingFailed extends DecodingFailureReason {
      def description: String = "protobuf decoding failed"
    }
  }

  // Helper constructors
  def unknownError(status: Status): GrpcError =
    UnknownError(status.code, status.message, status.details)

  def base64DecodingFailed(
    code: StatusCode,
    message: Option[String],
    rawHeaderValue: String
  ): GrpcError =
    StatusDetailsDecodingFailed(code, message, rawHeaderValue, DecodingFailureReason.Base64DecodingFailed)

  def protobufDecodingFailed(
    code: StatusCode,
    message: Option[String],
    rawHeaderValue: String
  ): GrpcError =
    StatusDetailsDecodingFailed(code, message, rawHeaderValue, DecodingFailureReason.ProtobufDecodingFailed)
}
