package smithy4s.example

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
  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(418),
  )

  val message = string.required[GenericClientError]("message", _.message).addHints(smithy.api.Required())

  implicit val schema: Schema[GenericClientError] = struct(
    message,
  ){
    GenericClientError.apply
  }.withId(ShapeId("smithy4s.example", "GenericClientError")).addHints(hints)
}
