package smithy4s.example

import smithy4s.example.Foo.schema
import smithy4s.schema.Schema._

case class NoMoreSpace(message: String, foo: Option[Foo]=None) extends Throwable {
  override def getMessage(): String = message
}
object NoMoreSpace extends smithy4s.ShapeTag.Companion[NoMoreSpace] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "NoMoreSpace")
  
  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(507),
  )
  
  implicit val schema: smithy4s.Schema[NoMoreSpace] = struct(
    string.required[NoMoreSpace]("message", _.message).addHints(smithy.api.Required()),
    Foo.schema.optional[NoMoreSpace]("foo", _.foo),
  ){
    NoMoreSpace.apply
  }.withId(id).addHints(hints)
}