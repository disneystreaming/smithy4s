package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RandomOtherServerError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object RandomOtherServerError extends ShapeTag.Companion[RandomOtherServerError] {
  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  )

  val message = string.optional[RandomOtherServerError]("message", _.message)

  implicit val schema: Schema[RandomOtherServerError] = struct(
    message,
  ){
    RandomOtherServerError.apply
  }.withId(ShapeId("smithy4s.example", "RandomOtherServerError")).addHints(hints)
}
