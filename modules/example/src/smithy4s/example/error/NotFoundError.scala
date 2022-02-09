package smithy4s.example.error

import smithy4s.syntax._

case class NotFoundError(error: Option[String] = None) extends Throwable {
}
object NotFoundError extends smithy4s.ShapeTag.Companion[NotFoundError] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example.error", "NotFoundError")

  val hints : smithy4s.Hints = smithy4s.Hints(
    id,
    smithy.api.Error.CLIENT,
    smithy.api.HttpError(404),
  )

  val schema: smithy4s.Schema[NotFoundError] = struct(
    string.optional[NotFoundError]("error", _.error),
  ){
    NotFoundError.apply
  }.withHints(hints)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[NotFoundError]] = schematic.Static(schema)
}