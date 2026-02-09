package smithy4s.grpc.http4s

import cats.implicits._
import org.typelevel.ci.CIString
import org.http4s.Header
import org.http4s.headers.`Content-Type`
import org.http4s.headers.Trailer
import smithy4s.grpc.CaseInsensitive
import smithy4s.grpc.GrpcStatus
import org.http4s.ParseFailure
import smithy4s.Blob
import java.util.Base64

object GrpcHeaders {
  val ContentType: `Content-Type` = org.http4s.headers.`Content-Type`
    .parse("application/grpc+proto")
    .getOrElse(throw new Throwable("Impossible: This protocol is valid"))

  val Trailers = Trailer(CIString("grpc-status"))

  // TODO  Content-Coding → "identity" / "gzip" / "deflate" / "snappy" / {custom}
  val GrpcEncoding: Header.Raw = Header.Raw(CIString("grpc-encoding"), "identity")

  val GrpcAcceptEncoding: Header.Raw =
    org.http4s.Header.Raw(CIString("grpc-accept-encoding"), "identity")

  val TE: Header.Raw = Header.Raw(CIString("te"), "trailers")

  def toSmithy4sHeader(headers: Header.ToRaw*): Map[CaseInsensitive, Seq[String]] = 
    headers
      .flatMap(_.values)
      .map(rawHeader => CaseInsensitive(rawHeader.name.toString) -> List(rawHeader.value))
      .toMap

  private val statusCodeParser = cats.parse.Numbers.nonNegativeIntString
    .mapFilter(s => GrpcStatus.fromStatusCode(s.toInt))

  implicit val grpcStatusHeader: Header[GrpcStatus, Header.Single] = Header.create(
    CIString("grpc-status"),
    (t: GrpcStatus) => t.code.toString(),
    (s: String) => statusCodeParser.parseAll(s).leftMap(e => ParseFailure("Invalid gRPC status", e.show))
  )

  implicit val grpcStatusDetailsBin: Header[Blob, Header.Single] = Header.create(
    CIString("grpc-status-details-bin"),
    (blob: Blob) => blob.toBase64String,
    (s: String) => Either.catchNonFatal(Base64.getDecoder().decode(s)).map(Blob(_)).leftMap[ParseFailure](e => ParseFailure("Invalid base64 encoded status details", e.getMessage))
  )


}
