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

  val three = string.required[DefaultOrderingTest]("three", _.three).addHints(smithy.api.Required())
  val one = int.required[DefaultOrderingTest]("one", _.one).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d)))
  val two = string.optional[DefaultOrderingTest]("two", _.two)

  implicit val schema: Schema[DefaultOrderingTest] = struct(
    three,
    one,
    two,
  ){
    DefaultOrderingTest.apply
  }.withId(id).addHints(hints)
}
