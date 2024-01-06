package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct

/** @param foo
  *   Helpful information for Foo
  *   int, bigInt and bDec are useful number constructs
  *   The string case is there because.
  */
final case class GetFooOutput(foo: Option[Foo] = None)

object GetFooOutput extends ShapeTag.Companion[GetFooOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetFooOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[GetFooOutput] = struct(
    Foo.schema.optional[GetFooOutput]("foo", _.foo),
  ){
    GetFooOutput.apply
  }.withId(id).addHints(hints)
}
