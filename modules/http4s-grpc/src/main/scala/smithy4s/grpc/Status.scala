package smithy4s
package grpc

import alloy.proto.StatusDetails

case class Status(code: StatusCode, message: Option[String], details: StatusDetails)

object Status {

  val ok: Status = Status(StatusCode.Ok, Option.empty, StatusDetails(List.empty))

}
