package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RandomOtherServerErrorWithCode(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object RandomOtherServerErrorWithCode extends ShapeTag.Companion[RandomOtherServerErrorWithCode] {
  val id: ShapeId = ShapeId("smithy4s.example", "RandomOtherServerErrorWithCode")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(503),
  )

  object Lenses {
    val message = Lens[RandomOtherServerErrorWithCode, Option[String]](_.message)(n => a => a.copy(message = n))
  }

  implicit val schema: Schema[RandomOtherServerErrorWithCode] = struct(
    string.optional[RandomOtherServerErrorWithCode]("message", _.message),
  ){
    RandomOtherServerErrorWithCode.apply
  }.withId(id).addHints(hints)
}
