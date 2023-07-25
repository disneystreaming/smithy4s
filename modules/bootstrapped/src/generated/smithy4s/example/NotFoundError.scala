package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NotFoundError(name: String) extends Throwable {
}
object NotFoundError extends ShapeTag.Companion[NotFoundError] {
  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(404),
  )

  val name = string.required[NotFoundError]("name", _.name).addHints(smithy.api.Required())

  implicit val schema: Schema[NotFoundError] = struct(
    name,
  ){
    NotFoundError.apply
  }.withId(ShapeId("smithy4s.example", "NotFoundError")).addHints(hints)
}
