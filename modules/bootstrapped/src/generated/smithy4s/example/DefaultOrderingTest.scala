package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string

final case class DefaultOrderingTest(three: String, one: Int = 1, two: Option[String] = None)

object DefaultOrderingTest extends ShapeTag.Companion[DefaultOrderingTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultOrderingTest")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[DefaultOrderingTest] = struct(
    string.required[DefaultOrderingTest]("three", _.three),
    int.field[DefaultOrderingTest]("one", _.one).addHints(smithy.api.Default(_root_.smithy4s.Document.fromDouble(1.0d))),
    string.optional[DefaultOrderingTest]("two", _.two),
  ){
    DefaultOrderingTest.apply
  }.withId(id).addHints(hints)
}
