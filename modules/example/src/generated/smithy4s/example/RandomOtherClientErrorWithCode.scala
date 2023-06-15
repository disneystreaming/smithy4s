package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RandomOtherClientErrorWithCode(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object RandomOtherClientErrorWithCode extends ShapeTag.Companion[RandomOtherClientErrorWithCode] {
  val id: ShapeId = ShapeId("smithy4s.example", "RandomOtherClientErrorWithCode")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(404),
  )

  implicit val schema: Schema[RandomOtherClientErrorWithCode] = struct(
    string.optional[RandomOtherClientErrorWithCode]("message", _.message),
  ){
    RandomOtherClientErrorWithCode.apply
  }.withId(id).addHints(hints)
}
