package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class RandomOtherClientError(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object RandomOtherClientError extends ShapeTag.Companion[RandomOtherClientError] {
  val id: ShapeId = ShapeId("smithy4s.example", "RandomOtherClientError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  implicit val schema: Schema[RandomOtherClientError] = struct(
    string.optional[RandomOtherClientError]("message", _.message),
  ){
    RandomOtherClientError.apply
  }.withId(id).addHints(hints)
}
