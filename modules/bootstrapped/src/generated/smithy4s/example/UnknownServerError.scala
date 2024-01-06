package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class UnknownServerError(errorCode: UnknownServerErrorCode, description: Option[String] = None, stateHash: Option[String] = None) extends Smithy4sThrowable {
}

object UnknownServerError extends ShapeTag.Companion[UnknownServerError] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnknownServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(500),
  )

  implicit val schema: Schema[UnknownServerError] = struct(
    UnknownServerErrorCode.schema.required[UnknownServerError]("errorCode", _.errorCode),
    string.optional[UnknownServerError]("description", _.description),
    string.optional[UnknownServerError]("stateHash", _.stateHash),
  ){
    UnknownServerError.apply
  }.withId(id).addHints(hints)
}
