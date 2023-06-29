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
  val id: ShapeId = ShapeId("smithy4s.example", "GenericClientError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(418),
  )

  implicit val schema: Schema[GenericClientError] = struct(
    string.required[GenericClientError]("message", _.message).addHints(smithy.api.Required()),
  ){
    GenericClientError.apply
  }.withId(id).addHints(hints)
}