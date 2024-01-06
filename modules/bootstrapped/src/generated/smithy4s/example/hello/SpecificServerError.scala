package smithy4s.example.hello

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class SpecificServerError(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object SpecificServerError extends ShapeTag.Companion[SpecificServerError] {
  val id: ShapeId = ShapeId("smithy4s.example.hello", "SpecificServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(599),
  )

  implicit val schema: Schema[SpecificServerError] = struct(
    string.optional[SpecificServerError]("message", _.message),
  ){
    SpecificServerError.apply
  }.withId(id).addHints(hints)
}
