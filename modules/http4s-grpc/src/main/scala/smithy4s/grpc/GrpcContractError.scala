package smithy4s
package grpc

import smithy4s.capability.MonadThrowLike
import smithy4s.codecs.PayloadError
import smithy4s.kinds.PolyFunction
import smithy4s.codecs.PayloadPath
import smithy4s.schema.Schema
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema._

sealed trait GrpcContractError
    extends Throwable
    with scala.util.control.NoStackTrace

object GrpcContractError {

  def fromPayloadError(payloadError: PayloadError): GrpcContractError =
    GrpcPayloadError(
      payloadError.path,
      payloadError.expected,
      payloadError.message
    )

  def fromPayloadErrorK[F[_]: MonadThrowLike]: PolyFunction[F, F] =
    MonadThrowLike.mapErrorK[F] { case e: PayloadError => fromPayloadError(e) }

  val schema: Schema[GrpcContractError] = {
    //FIXME: figure this out
    val payload = GrpcPayloadError.schema.oneOf[GrpcContractError]("payload")
    // val metadata = MetadataError.schema.oneOf[HttpContractError]("metadata")
    union(payload) {
      case _: GrpcPayloadError => 0
      // case _: MetadataError    => 1
    }
  }
}

case class GrpcPayloadError(
		path: PayloadPath,
		expected: String,
		message: String
) extends GrpcContractError {
	override def toString(): String =
		s"GrpcPayloadError($path, expected = $expected, message=$message)"
	override def getMessage(): String = s"$message (path: $path)"
}

object GrpcPayloadError {
  val schema: Schema[GrpcPayloadError] = {
    val path = PayloadPath.schema.required[grpc.GrpcPayloadError]("path", _.path)
    val expected = string.required[grpc.GrpcPayloadError]("expected", _.expected)
    val message = string.required[grpc.GrpcPayloadError]("message", _.message)
    struct(path, expected, message)(GrpcPayloadError.apply)
  }
}