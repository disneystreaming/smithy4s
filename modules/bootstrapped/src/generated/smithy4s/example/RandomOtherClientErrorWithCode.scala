package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class RandomOtherClientErrorWithCode(message: Option[String] = None) extends Smithy4sThrowable {
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
