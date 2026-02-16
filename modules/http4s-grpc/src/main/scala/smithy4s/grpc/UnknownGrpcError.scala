package smithy4s.grpc

import alloy.proto.StatusDetails

case class UnknownGrpcError(code: StatusCode, message: Option[String], details: StatusDetails) extends Throwable {
  override def getMessage(): String = {
    val detailsString = 
      details
        .value
        .map{detailsEntry => s"{type=${detailsEntry.typeUrl}, errorPayloadAsBase64=${detailsEntry.bytes.toBase64String}"}
        .mkString("[", "," , "]")
    s"Status: ${code.value}, message: ${message.mkString}, details: $detailsString"
  }
}

object UnknownGrpcError {
  def fromGrpcStatus(status: Status): UnknownGrpcError =
    UnknownGrpcError(status.code, status.message, status.details)
}