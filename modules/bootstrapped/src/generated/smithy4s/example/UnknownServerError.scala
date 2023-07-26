package smithy4s.example

import smithy.api.Error
import smithy.api.HttpError
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class UnknownServerError(errorCode: UnknownServerErrorCode, description: Option[String] = None, stateHash: Option[String] = None) extends Throwable {
}
object UnknownServerError extends ShapeTag.Companion[UnknownServerError] {

  val errorCode = UnknownServerErrorCode.schema.required[UnknownServerError]("errorCode", _.errorCode, n => c => c.copy(errorCode = n)).addHints(Required())
  val description = string.optional[UnknownServerError]("description", _.description, n => c => c.copy(description = n))
  val stateHash = string.optional[UnknownServerError]("stateHash", _.stateHash, n => c => c.copy(stateHash = n))

  implicit val schema: Schema[UnknownServerError] = struct(
    errorCode,
    description,
    stateHash,
  ){
    UnknownServerError.apply
  }
  .withId(ShapeId("smithy4s.example", "UnknownServerError"))
  .addHints(
    Hints(
      Error.SERVER.widen,
      HttpError(500),
    )
  )
}
