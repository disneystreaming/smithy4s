package smithy4s.example.hello

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class GenericServerError(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object GenericServerError extends ShapeTag.Companion[GenericServerError] {
  val id: ShapeId = ShapeId("smithy4s.example.hello", "GenericServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(500),
  )

  implicit val schema: Schema[GenericServerError] = struct(
    string.optional[GenericServerError]("message", _.message),
  ){
    GenericServerError.apply
  }.withId(id).addHints(hints)
}
