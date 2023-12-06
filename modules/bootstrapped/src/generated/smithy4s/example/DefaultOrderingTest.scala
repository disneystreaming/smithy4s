package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class DefaultOrderingTest(three: String, one: Int = 1, two: Option[String] = None)

object DefaultOrderingTest extends ShapeTag.Companion[DefaultOrderingTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultOrderingTest")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[DefaultOrderingTest] = struct(
    string.required[DefaultOrderingTest]("three", _.three),
    int.field[DefaultOrderingTest]("one", _.one).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    string.optional[DefaultOrderingTest]("two", _.two),
  ){
    DefaultOrderingTest.apply
  }.withId(id).addHints(hints)
}
