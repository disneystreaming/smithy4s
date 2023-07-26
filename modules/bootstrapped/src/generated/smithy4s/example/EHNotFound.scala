package smithy4s.example

import scala.util.control.NoStackTrace
import smithy.api.Error
import smithy.api.HttpError
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EHNotFound(message: Option[String] = None) extends NoStackTrace {
  override def getMessage(): String = message.orNull
}
object EHNotFound extends ShapeTag.$Companion[EHNotFound] {
  val $id: ShapeId = ShapeId("smithy4s.example", "EHNotFound")

  val $hints: Hints = Hints(
    Error.CLIENT.widen,
    HttpError(404),
  )

  val message: FieldLens[EHNotFound, Option[String]] = string.optional[EHNotFound]("message", _.message, n => c => c.copy(message = n))

  implicit val $schema: Schema[EHNotFound] = struct(
    message,
  ){
    EHNotFound.apply
  }.withId($id).addHints($hints)
}
