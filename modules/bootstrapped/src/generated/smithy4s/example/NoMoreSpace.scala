package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

/** @param foo
  *   Helpful information for Foo
  *   int, bigInt and bDec are useful number constructs
  *   The string case is there because.
  */
final case class NoMoreSpace(message: String, foo: Option[Foo] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message
}

object NoMoreSpace extends ShapeTag.Companion[NoMoreSpace] {
  val id: ShapeId = ShapeId("smithy4s.example", "NoMoreSpace")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(507),
  )

  implicit val schema: Schema[NoMoreSpace] = struct(
    string.required[NoMoreSpace]("message", _.message),
    Foo.schema.optional[NoMoreSpace]("foo", _.foo),
  ){
    NoMoreSpace.apply
  }.withId(id).addHints(hints)
}
