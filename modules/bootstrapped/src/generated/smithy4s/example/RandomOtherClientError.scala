package smithy4s.example

import smithy.api.Error
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RandomOtherClientError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object RandomOtherClientError extends ShapeTag.Companion[RandomOtherClientError] {

  val message = string.optional[RandomOtherClientError]("message", _.message, n => c => c.copy(message = n))

  implicit val schema: Schema[RandomOtherClientError] = struct(
    message,
  ){
    RandomOtherClientError.apply
  }
  .withId(ShapeId("smithy4s.example", "RandomOtherClientError"))
  .addHints(
    Hints(
      Error.CLIENT.widen,
    )
  )
}
