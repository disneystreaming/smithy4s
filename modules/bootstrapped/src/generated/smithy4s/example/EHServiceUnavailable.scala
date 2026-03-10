package smithy4s.example

import scala.util.control.NoStackTrace
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EHServiceUnavailable(message: Option[String] = None) extends NoStackTrace {
  override def getMessage(): String = message.orNull
}

object EHServiceUnavailable extends ShapeTag.Companion[EHServiceUnavailable] {
  val id: ShapeId = ShapeId("smithy4s.example", "EHServiceUnavailable")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "error"), smithy4s.Document.fromString("server")),
    Hints.dynamic(ShapeId("smithy.api", "httpError"), smithy4s.Document.fromLong(503L)),
  )

  // constructor using the original order from the spec
  private def make(message: Option[String]): EHServiceUnavailable = EHServiceUnavailable(message)

  implicit val schema: Schema[EHServiceUnavailable] = struct[EHServiceUnavailable](
    string.optional[EHServiceUnavailable]("message", _.message),
  )(make).withId(id).addHints(hints)
}
