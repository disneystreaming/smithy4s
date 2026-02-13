package smithy4s.grpc.http4s

import cats.implicits._
import org.typelevel.ci.CIString
import smithy4s.grpc.GrpcStatus
import org.http4s.ParseFailure

object GrpcHeaders {

  trait Header {
    def name: String
    def ciName: CIString = CIString(name)
  }

  object Status extends Header {
    val name = "grpc-status"

    def parse(value: String) = 
      cats.parse.Numbers.nonNegativeIntString
        .map(s => GrpcStatus.fromStatusCode(s.toInt))
        .parseAll(value)
        .leftMap(e => ParseFailure("Invalid gRPC status", e.show))
  }

  object Message extends Header {
    val name = "grpc-message"
  }
  
  object StatusDetailsBin extends Header {
    val name = "grpc-status-details-bin"
  }

}
