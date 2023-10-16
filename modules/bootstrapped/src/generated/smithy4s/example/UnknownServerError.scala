package smithy4s.example

import scala.runtime.ScalaRunTime
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class UnknownServerError(errorCode: UnknownServerErrorCode, description: Option[String] = None, stateHash: Option[String] = None) extends Throwable {
  override def toString(): String = ScalaRunTime._toString(this)
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
