package smithy4s.example

import scala.util.control.NoStackTrace
import smithy.api.Error
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EHFallbackClientError(message: Option[String] = None) extends NoStackTrace {
  override def getMessage(): String = message.orNull
}
object EHFallbackClientError extends ShapeTag.$Companion[EHFallbackClientError] {
  val $id: ShapeId = ShapeId("smithy4s.example", "EHFallbackClientError")

  val $hints: Hints = Hints(
    Error.CLIENT.widen,
  )

  val message: FieldLens[EHFallbackClientError, Option[String]] = string.optional[EHFallbackClientError]("message", _.message, n => c => c.copy(message = n))

  implicit val $schema: Schema[EHFallbackClientError] = struct(
    message,
  ){
    EHFallbackClientError.apply
  }.withId($id).addHints($hints)
}
