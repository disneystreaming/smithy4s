package smithy4s.protobuf

case class ProtobufReadError(cause: Throwable) extends Throwable {
  override def getMessage() = "Failed to decode protobuf message"
  override def getCause(): Throwable = cause
}
