package smithy4s.example

import smithy.api.Error
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RandomOtherServerError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object RandomOtherServerError extends ShapeTag.Companion[RandomOtherServerError] {

  val message: FieldLens[RandomOtherServerError, Option[String]] = string.optional[RandomOtherServerError]("message", _.message, n => c => c.copy(message = n))

  implicit val schema: Schema[RandomOtherServerError] = struct(
    message,
  ){
    RandomOtherServerError.apply
  }
  .withId(ShapeId("smithy4s.example", "RandomOtherServerError"))
  .addHints(
    Error.SERVER.widen,
  )
}
