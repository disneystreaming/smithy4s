package smithy4s.example.error

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class NotFoundError(error: Option[String] = None) extends Smithy4sThrowable {
}

object NotFoundError extends ShapeTag.Companion[NotFoundError] {
  val id: ShapeId = ShapeId("smithy4s.example.error", "NotFoundError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(404),
  )

  implicit val schema: Schema[NotFoundError] = struct(
    string.optional[NotFoundError]("error", _.error),
  ){
    NotFoundError.apply
  }.withId(id).addHints(hints)
}
