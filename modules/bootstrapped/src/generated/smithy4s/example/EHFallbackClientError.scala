package smithy4s.example

import scala.util.control.NoStackTrace
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EHFallbackClientError(message: Option[String] = None) extends NoStackTrace {
  override def getMessage(): String = message.orNull
}

object EHFallbackClientError extends ShapeTag.Companion[EHFallbackClientError] {
  val id: ShapeId = ShapeId("smithy4s.example", "EHFallbackClientError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  ).lazily

  // constructor using the original order from the spec
  private def make(message: Option[String]): EHFallbackClientError = EHFallbackClientError(message)

  implicit val schema: Schema[EHFallbackClientError] = struct(
    string.optional[EHFallbackClientError]("message", _.message),
  )(make).withId(id).addHints(hints)
}
