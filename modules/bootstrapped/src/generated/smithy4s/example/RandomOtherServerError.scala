package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class RandomOtherServerError(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object RandomOtherServerError extends ShapeTag.Companion[RandomOtherServerError] {
  val id: ShapeId = ShapeId("smithy4s.example", "RandomOtherServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  )

  implicit val schema: Schema[RandomOtherServerError] = struct(
    string.optional[RandomOtherServerError]("message", _.message),
  ){
    RandomOtherServerError.apply
  }.withId(id).addHints(hints)
}
