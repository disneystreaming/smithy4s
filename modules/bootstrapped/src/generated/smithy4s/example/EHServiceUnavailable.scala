package smithy4s.example

import _root_.scala.util.control.NoStackTrace
import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class EHServiceUnavailable(message: Option[String] = None) extends NoStackTrace {
  override def getMessage(): String = message.orNull
}

object EHServiceUnavailable extends ShapeTag.Companion[EHServiceUnavailable] {
  val id: ShapeId = ShapeId("smithy4s.example", "EHServiceUnavailable")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(503),
  )

  implicit val schema: Schema[EHServiceUnavailable] = struct(
    string.optional[EHServiceUnavailable]("message", _.message),
  ){
    EHServiceUnavailable.apply
  }.withId(id).addHints(hints)
}
