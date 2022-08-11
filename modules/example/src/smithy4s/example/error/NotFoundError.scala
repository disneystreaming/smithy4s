package smithy4s.example.error

import smithy4s._
import smithy4s.schema.Schema._

case class NotFoundError(error: Option[String]=None) extends Throwable {
  
}
object NotFoundError extends ShapeTag.Companion[NotFoundError] {
  val id: ShapeId = ShapeId("smithy4s.example.error", "NotFoundError")
  
  val hints : Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(404),
  )
  
  implicit val schema: Schema[NotFoundError] = struct(
    string.optional[NotFoundError]("error", _.error),
  ){
    NotFoundError.apply
  }.withId(id).addHints(hints)
}