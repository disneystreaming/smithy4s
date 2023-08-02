package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

sealed trait TestIdRefUnion extends scala.Product with scala.Serializable {
  @inline final def widen: TestIdRefUnion = this
  def $ordinal: Int
}
object TestIdRefUnion extends ShapeTag.Companion[TestIdRefUnion] {

  def test(test:ShapeId): TestIdRefUnion = TestCase(test)
  def testTwo(testTwo:TestIdRefTwo): TestIdRefUnion = TestTwoCase(testTwo)

  val id: ShapeId = ShapeId("smithy4s.example", "TestIdRefUnion")

  val hints: Hints = Hints.empty

  final case class TestCase(test: ShapeId) extends TestIdRefUnion { final def $ordinal: Int = 0 }
  final case class TestTwoCase(testTwo: TestIdRefTwo) extends TestIdRefUnion { final def $ordinal: Int = 1 }

  object TestCase {
    val hints: Hints = Hints.empty
    val schema: Schema[TestCase] = bijection(string.refined[ShapeId](smithy.api.IdRef(selector = "*", failWhenMissing = None, errorMessage = None)).addHints(hints), TestCase(_), _.test)
    val alt = schema.oneOf[TestIdRefUnion]("test")
  }
  object TestTwoCase {
    val hints: Hints = Hints.empty
    val schema: Schema[TestTwoCase] = bijection(TestIdRefTwo.schema.addHints(hints), TestTwoCase(_), _.testTwo)
    val alt = schema.oneOf[TestIdRefUnion]("testTwo")
  }

  implicit val schema: Schema[TestIdRefUnion] = union(
    TestCase.alt,
    TestTwoCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
