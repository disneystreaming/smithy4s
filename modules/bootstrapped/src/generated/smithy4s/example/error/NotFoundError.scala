package smithy4s.example.error

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NotFoundError(error: Option[String] = None) extends Smithy4sThrowable

object NotFoundError extends ShapeTag.Companion[NotFoundError] {
  val id: ShapeId = ShapeId("smithy4s.example.error", "NotFoundError")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "error"), smithy4s.Document.fromString("client")),
    Hints.dynamic(ShapeId("smithy.api", "httpError"), smithy4s.Document.fromLong(404L)),
  )

  // constructor using the original order from the spec
  private def make(error: Option[String]): NotFoundError = NotFoundError(error)

  implicit val schema: Schema[NotFoundError] = struct[NotFoundError](
    string.optional[NotFoundError]("error", _.error),
  )(make).withId(id).addHints(hints)
}
