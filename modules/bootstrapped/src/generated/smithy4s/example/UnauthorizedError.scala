package smithy4s.example

import scala.runtime.ScalaRunTime
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class UnauthorizedError(reason: String) extends Throwable {
  override def toString(): String = ScalaRunTime._toString(this)
}

object UnauthorizedError extends ShapeTag.Companion[UnauthorizedError] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnauthorizedError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  implicit val schema: Schema[UnauthorizedError] = struct(
    string.required[UnauthorizedError]("reason", _.reason),
  ){
    UnauthorizedError.apply
  }.withId(id).addHints(hints)
}
