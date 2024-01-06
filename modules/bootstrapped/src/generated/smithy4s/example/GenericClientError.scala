package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class GenericClientError(message: String) extends Smithy4sThrowable {
  override def getMessage(): String = message
}

object GenericClientError extends ShapeTag.Companion[GenericClientError] {
  val id: ShapeId = ShapeId("smithy4s.example", "GenericClientError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(418),
  )

  implicit val schema: Schema[GenericClientError] = struct(
    string.required[GenericClientError]("message", _.message),
  ){
    GenericClientError.apply
  }.withId(id).addHints(hints)
}
