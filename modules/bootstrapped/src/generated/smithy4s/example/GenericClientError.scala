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

final case class GenericClientError(message: String) extends Throwable {
  override def getMessage(): String = message
}
object GenericClientError extends ShapeTag.Companion[GenericClientError] {

  val message = string.required[GenericClientError]("message", _.message, n => c => c.copy(message = n)).addHints(Required())

  implicit val schema: Schema[GenericClientError] = struct(
    message,
  ){
    GenericClientError.apply
  }
  .withId(ShapeId("smithy4s.example", "GenericClientError"))
  .addHints(
    Hints(
      Error.CLIENT.widen,
      HttpError(418),
    )
  )
}
