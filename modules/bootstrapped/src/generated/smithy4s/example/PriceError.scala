package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string

final case class PriceError(message: String, code: Int) extends Smithy4sThrowable {
  override def getMessage(): String = message
}

object PriceError extends ShapeTag.Companion[PriceError] {
  val id: ShapeId = ShapeId("smithy4s.example", "PriceError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  implicit val schema: Schema[PriceError] = struct(
    string.required[PriceError]("message", _.message),
    int.required[PriceError]("code", _.code).addHints(smithy.api.HttpHeader("X-CODE")),
  ){
    PriceError.apply
  }.withId(id).addHints(hints)
}
