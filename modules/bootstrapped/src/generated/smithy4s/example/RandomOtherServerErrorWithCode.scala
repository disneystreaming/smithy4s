package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RandomOtherServerErrorWithCode(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object RandomOtherServerErrorWithCode extends ShapeTag.Companion[RandomOtherServerErrorWithCode] {
  val id: ShapeId = ShapeId("smithy4s.example", "RandomOtherServerErrorWithCode")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(503),
  ).lazily

  // constructor using the original order from the spec
  private def make(message: Option[String]): RandomOtherServerErrorWithCode = RandomOtherServerErrorWithCode(message)

  implicit val schema: Schema[RandomOtherServerErrorWithCode] = struct(
    string.optional[RandomOtherServerErrorWithCode]("message", _.message),
  ){
    make
  }.withId(id).addHints(hints)
}
