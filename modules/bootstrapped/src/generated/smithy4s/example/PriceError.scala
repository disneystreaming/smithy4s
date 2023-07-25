package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class PriceError(message: String, code: Int) extends Throwable {
  override def getMessage(): String = message
}
object PriceError extends ShapeTag.Companion[PriceError] {
  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  val message = string.required[PriceError]("message", _.message).addHints(smithy.api.Required())
  val code = int.required[PriceError]("code", _.code).addHints(smithy.api.HttpHeader("X-CODE"), smithy.api.Required())

  implicit val schema: Schema[PriceError] = struct(
    message,
    code,
  ){
    PriceError.apply
  }.withId(ShapeId("smithy4s.example", "PriceError")).addHints(hints)
}
