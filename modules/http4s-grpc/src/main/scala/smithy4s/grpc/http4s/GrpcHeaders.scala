package smithy4s.grpc.http4s

import cats.implicits._
import org.typelevel.ci.CIString
import org.http4s.ParseFailure
import smithy4s.grpc.StatusCode
import org.http4s.Headers

object GrpcHeaders {

  def getSingle(headers: Headers, header: Header): Option[String] =
    headers.get(header.ciName).map(_.head.value)

  trait Header {
    def name: String
    def ciName: CIString = CIString(name)
  }

  object Status extends Header {
    val name = "grpc-status"

    def parse(value: String): Either[ParseFailure, StatusCode] = 
      cats.parse.Numbers.nonNegativeIntString
        .map(s => StatusCode.fromStatusCode(s.toInt))
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
