package smithy4s.example

import smithy.api.Error
import smithy.api.HttpError
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EHServiceUnavailable(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object EHServiceUnavailable extends ShapeTag.$Companion[EHServiceUnavailable] {
  val $id: ShapeId = ShapeId("smithy4s.example", "EHServiceUnavailable")

  val $hints: Hints = Hints(
    Error.SERVER.widen,
    HttpError(503),
  )

  val message: FieldLens[EHServiceUnavailable, Option[String]] = string.optional[EHServiceUnavailable]("message", _.message, n => c => c.copy(message = n))

  implicit val $schema: Schema[EHServiceUnavailable] = struct(
    message,
  ){
    EHServiceUnavailable.apply
  }.withId($id).addHints($hints)
}
