package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class PriceError(message: String, code: Int) extends Throwable {
  override def getMessage(): String = message
}
object PriceError extends ShapeTag.Companion[PriceError] {
  val id: ShapeId = ShapeId("smithy4s.example", "PriceError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  object Optics {
    val message = Lens[PriceError, String](_.message)(n => a => a.copy(message = n))
    val code = Lens[PriceError, Int](_.code)(n => a => a.copy(code = n))
  }

  implicit val schema: Schema[PriceError] = struct(
    string.required[PriceError]("message", _.message).addHints(smithy.api.Required()),
    int.required[PriceError]("code", _.code).addHints(smithy.api.HttpHeader("X-CODE"), smithy.api.Required()),
  ){
    PriceError.apply
  }.withId(id).addHints(hints)
}
