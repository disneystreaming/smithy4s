package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** @param foo
  *   Helpful information for Foo
  *   int, bigInt and bDec are useful number constructs
  *   The string case is there because.
  */
final case class NoMoreSpace(message: String, foo: Option[Foo] = None) extends Throwable {
  override def getMessage(): String = message
}
object NoMoreSpace extends ShapeTag.Companion[NoMoreSpace] {
  val id: ShapeId = ShapeId("smithy4s.example", "NoMoreSpace")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(507),
  )

  implicit val schema: Schema[NoMoreSpace] = struct(
    string.required[NoMoreSpace]("message", _.message).addHints(smithy.api.Required()),
    Foo.schema.optional[NoMoreSpace]("foo", _.foo),
  ){
    NoMoreSpace.apply
  }.withId(id).addHints(hints)
}