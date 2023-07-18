package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RandomOtherClientError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object RandomOtherClientError extends ShapeTag.Companion[RandomOtherClientError] {
  val id: ShapeId = ShapeId("smithy4s.example", "RandomOtherClientError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  object Lenses {
    val message = Lens[RandomOtherClientError, Option[String]](_.message)(n => a => a.copy(message = n))
  }

  implicit val schema: Schema[RandomOtherClientError] = struct(
    string.optional[RandomOtherClientError]("message", _.message),
  ){
    RandomOtherClientError.apply
  }.withId(id).addHints(hints)
}
