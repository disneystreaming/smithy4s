package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class RandomOtherServerErrorWithCode(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object RandomOtherServerErrorWithCode extends ShapeTag.Companion[RandomOtherServerErrorWithCode] {
  val id: ShapeId = ShapeId("smithy4s.example", "RandomOtherServerErrorWithCode")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(503),
  )

  implicit val schema: Schema[RandomOtherServerErrorWithCode] = struct(
    string.optional[RandomOtherServerErrorWithCode]("message", _.message),
  ){
    RandomOtherServerErrorWithCode.apply
  }.withId(id).addHints(hints)
}
