package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RandomOtherClientError(message: Option[java.lang.String] = None) extends Throwable {
  override def getMessage(): scala.Predef.String = message.orNull
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
