package smithy4s.example

import smithy.api.Error
import smithy.api.HttpError
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RandomOtherServerErrorWithCode(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object RandomOtherServerErrorWithCode extends ShapeTag.Companion[RandomOtherServerErrorWithCode] {

  val message = string.optional[RandomOtherServerErrorWithCode]("message", _.message, n => c => c.copy(message = n))

  implicit val schema: Schema[RandomOtherServerErrorWithCode] = struct(
    message,
  ){
    RandomOtherServerErrorWithCode.apply
  }
  .withId(ShapeId("smithy4s.example", "RandomOtherServerErrorWithCode"))
  .addHints(
    Hints(
      Error.SERVER.widen,
      HttpError(503),
    )
  )
}
