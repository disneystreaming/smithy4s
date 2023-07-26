package smithy4s.example.hello

import smithy.api.Error
import smithy.api.HttpError
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SpecificServerError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object SpecificServerError extends ShapeTag.Companion[SpecificServerError] {

  val message: FieldLens[SpecificServerError, Option[String]] = string.optional[SpecificServerError]("message", _.message, n => c => c.copy(message = n))

  implicit val schema: Schema[SpecificServerError] = struct(
    message,
  ){
    SpecificServerError.apply
  }
  .withId(ShapeId("smithy4s.example.hello", "SpecificServerError"))
  .addHints(
    Error.SERVER.widen,
    HttpError(599),
  )
}
