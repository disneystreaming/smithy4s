package smithy4s.example

import smithy.api.Default
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class DefaultOrderingTest(three: String, one: Int = 1, two: Option[String] = None)
object DefaultOrderingTest extends ShapeTag.$Companion[DefaultOrderingTest] {
  val $id: ShapeId = ShapeId("smithy4s.example", "DefaultOrderingTest")

  val $hints: Hints = Hints.empty

  val three: FieldLens[DefaultOrderingTest, String] = string.required[DefaultOrderingTest]("three", _.three, n => c => c.copy(three = n)).addHints(Required())
  val one: FieldLens[DefaultOrderingTest, Int] = int.required[DefaultOrderingTest]("one", _.one, n => c => c.copy(one = n)).addHints(Default(smithy4s.Document.fromDouble(1.0d)))
  val two: FieldLens[DefaultOrderingTest, Option[String]] = string.optional[DefaultOrderingTest]("two", _.two, n => c => c.copy(two = n))

  implicit val $schema: Schema[DefaultOrderingTest] = struct(
    three,
    one,
    two,
  ){
    DefaultOrderingTest.apply
  }.withId($id).addHints($hints)
}
