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

final case class NotFoundError(name: String) extends Throwable {
}
object NotFoundError extends ShapeTag.Companion[NotFoundError] {

  val name = string.required[NotFoundError]("name", _.name, n => c => c.copy(name = n)).addHints(Required())

  implicit val schema: Schema[NotFoundError] = struct(
    name,
  ){
    NotFoundError.apply
  }
  .withId(ShapeId("smithy4s.example", "NotFoundError"))
  .addHints(
    Hints(
      Error.CLIENT.widen,
      HttpError(404),
    )
  )
}
