package smithy4s.example.error

import smithy.api.Error
import smithy.api.HttpError
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NotFoundError(error: Option[String] = None) extends Throwable {
}
object NotFoundError extends ShapeTag.$Companion[NotFoundError] {
  val $id: ShapeId = ShapeId("smithy4s.example.error", "NotFoundError")

  val $hints: Hints = Hints(
    Error.CLIENT.widen,
    HttpError(404),
  )

  val error: FieldLens[NotFoundError, Option[String]] = string.optional[NotFoundError]("error", _.error, n => c => c.copy(error = n))

  implicit val $schema: Schema[NotFoundError] = struct(
    error,
  ){
    NotFoundError.apply
  }.withId($id).addHints($hints)
}