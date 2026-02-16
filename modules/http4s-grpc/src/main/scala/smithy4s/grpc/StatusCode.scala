package smithy4s
package grpc

sealed abstract class StatusCode(val value: Int) extends  Product with Serializable

object StatusCode {

  sealed abstract class Failed(value: Int) extends StatusCode(value)
  case object Ok extends StatusCode(value = 0)
  case object Cancelled extends Failed(value = 1)
  case object Unknown extends Failed(value = 2)
  case object InvalidArgument extends Failed(value = 3)
  case object DeadlineExceeded extends Failed(value = 4)
  case object NotFound extends Failed(value = 5)
  case object AlreadyExists extends Failed(value = 6)
  case object PermissionDenied extends Failed(value = 7)
  case object ResourceExhausted extends Failed(value = 8)
  case object FailedPrecondition extends Failed(value = 9)
  case object Aborted extends Failed(value = 10)
  case object OutOfRange extends Failed(value = 11)
  case object Unimplemented extends Failed(value = 12)
  case object Internal extends Failed(value = 13)
  case object Unavailable extends Failed(value = 14)
  case object DataLoss extends Failed(value = 15)  
  case object Unauthenticated extends Failed(value = 16)

  private lazy val statusByCode: Map[Int, StatusCode] = List(
    Ok,
    Cancelled,
    Unknown,
    InvalidArgument,
    DeadlineExceeded,
    NotFound,
    AlreadyExists,
    PermissionDenied,
    ResourceExhausted,
    FailedPrecondition,
    Aborted,
    OutOfRange,
    Unimplemented,
    Internal,
    Unavailable,
    DataLoss,
    Unauthenticated
  ).map(s => (s.value, s))
  .toMap

  def fromStatusCode(statusCode: Int): StatusCode = 
    statusByCode
      .get(statusCode)
      // https://github.com/grpc/grpc/blob/master/doc/statuscodes.md
      // All RPCs started at a client return a status object composed of an integer
      // code and a string message.
      // The server-side can choose the status it returns for a given RPC.
      // Applications should only use values defined above.
      // gRPC libraries that encounter values outside this range must either propagate 
      // them directly or convert them to UNKNOWN.
      .getOrElse(Unknown) // we chose to default to UNKNOWN

}
