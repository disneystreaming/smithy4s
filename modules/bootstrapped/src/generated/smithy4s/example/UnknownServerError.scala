package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class UnknownServerError(errorCode: UnknownServerErrorCode, description: Option[String] = None, stateHash: Option[String] = None) extends Throwable {
}
object UnknownServerError extends ShapeTag.Companion[UnknownServerError] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnknownServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(500),
  )

  val errorCode = UnknownServerErrorCode.schema.required[UnknownServerError]("errorCode", _.errorCode).addHints(smithy.api.Required())
  val description = string.optional[UnknownServerError]("description", _.description)
  val stateHash = string.optional[UnknownServerError]("stateHash", _.stateHash)

  implicit val schema: Schema[UnknownServerError] = struct(
    errorCode,
    description,
    stateHash,
  ){
    UnknownServerError.apply
  }.withId(id).addHints(hints)
}
