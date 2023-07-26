package smithy4s.example

import smithy.api.Error
import smithy.api.HttpHeader
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class PriceError(message: String, code: Int) extends Throwable {
  override def getMessage(): String = message
}
object PriceError extends ShapeTag.$Companion[PriceError] {
  val $id: ShapeId = ShapeId("smithy4s.example", "PriceError")

  val $hints: Hints = Hints(
    Error.CLIENT.widen,
  )

  val message: FieldLens[PriceError, String] = string.required[PriceError]("message", _.message, n => c => c.copy(message = n)).addHints(Required())
  val code: FieldLens[PriceError, Int] = int.required[PriceError]("code", _.code, n => c => c.copy(code = n)).addHints(HttpHeader("X-CODE"), Required())

  implicit val $schema: Schema[PriceError] = struct(
    message,
    code,
  ){
    PriceError.apply
  }.withId($id).addHints($hints)
}
