package smithy4s.example

import _root_.scala.util.control.NoStackTrace
import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class EHFallbackClientError(message: Option[String] = None) extends NoStackTrace {
  override def getMessage(): String = message.orNull
}

object EHFallbackClientError extends ShapeTag.Companion[EHFallbackClientError] {
  val id: ShapeId = ShapeId("smithy4s.example", "EHFallbackClientError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  implicit val schema: Schema[EHFallbackClientError] = struct(
    string.optional[EHFallbackClientError]("message", _.message),
  ){
    EHFallbackClientError.apply
  }.withId(id).addHints(hints)
}
