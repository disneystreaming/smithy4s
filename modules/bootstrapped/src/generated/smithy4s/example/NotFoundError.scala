package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class NotFoundError(name: String) extends Smithy4sThrowable {
}

object NotFoundError extends ShapeTag.Companion[NotFoundError] {
  val id: ShapeId = ShapeId("smithy4s.example", "NotFoundError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(404),
  )

  implicit val schema: Schema[NotFoundError] = struct(
    string.required[NotFoundError]("name", _.name),
  ){
    NotFoundError.apply
  }.withId(id).addHints(hints)
}
