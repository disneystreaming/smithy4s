package smithy4s.example

import smithy.api.Default
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class DefaultOrderingTest(three: String, one: Int = 1, two: Option[String] = None)
object DefaultOrderingTest extends ShapeTag.Companion[DefaultOrderingTest] {

  val three = string.required[DefaultOrderingTest]("three", _.three, n => c => c.copy(three = n)).addHints(Required())
  val one = int.required[DefaultOrderingTest]("one", _.one, n => c => c.copy(one = n)).addHints(Default(smithy4s.Document.fromDouble(1.0d)))
  val two = string.optional[DefaultOrderingTest]("two", _.two, n => c => c.copy(two = n))

  implicit val schema: Schema[DefaultOrderingTest] = struct(
    three,
    one,
    two,
  ){
    DefaultOrderingTest.apply
  }
  .withId(ShapeId("smithy4s.example", "DefaultOrderingTest"))
  .addHints(
    Hints.empty
  )
}
