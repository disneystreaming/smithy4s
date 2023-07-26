package smithy4s.example

import smithy.api.Error
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EHFallbackServerError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object EHFallbackServerError extends ShapeTag.$Companion[EHFallbackServerError] {
  val $id: ShapeId = ShapeId("smithy4s.example", "EHFallbackServerError")

  val $hints: Hints = Hints(
    Error.SERVER.widen,
  )

  val message: FieldLens[EHFallbackServerError, Option[String]] = string.optional[EHFallbackServerError]("message", _.message, n => c => c.copy(message = n))

  implicit val $schema: Schema[EHFallbackServerError] = struct(
    message,
  ){
    EHFallbackServerError.apply
  }.withId($id).addHints($hints)
}
