package smithy4s.grpc.http4s

import cats.implicits._
import org.typelevel.ci.CIString
import smithy4s.grpc.GrpcStatus
import org.http4s.ParseFailure

object GrpcHeaders {

  object Status {
    val name = "grpc-status"
    val ciName = CIString(name)

    def parse(value: String) = 
      cats.parse.Numbers.nonNegativeIntString
        .map(s => GrpcStatus.fromStatusCode(s.toInt))
        .parseAll(value)
        .leftMap(e => ParseFailure("Invalid gRPC status", e.show))
  }

  val message = "grpc-message"
  
  object StatusDetailsBin {
    val name = "grpc-status-details-bin"
  }

}
