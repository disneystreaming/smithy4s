package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RandomOtherServerError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object RandomOtherServerError extends ShapeTag.Companion[RandomOtherServerError] {
  val id: ShapeId = ShapeId("smithy4s.example", "RandomOtherServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  )

  object Lenses {
    val message = Lens[RandomOtherServerError, Option[String]](_.message)(n => a => a.copy(message = n))
  }

  implicit val schema: Schema[RandomOtherServerError] = struct(
    string.optional[RandomOtherServerError]("message", _.message),
  ){
    RandomOtherServerError.apply
  }.withId(id).addHints(hints)
}
