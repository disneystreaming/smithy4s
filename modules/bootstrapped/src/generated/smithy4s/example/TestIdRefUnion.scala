package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

sealed trait TestIdRefUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: TestIdRefUnion = this
  def $ordinal: Int

  object project {
    def test: Option[ShapeId] = TestIdRefUnion.TestCase.alt.project.lift(self).map(_.test)
    def testTwo: Option[TestIdRefTwo] = TestIdRefUnion.TestTwoCase.alt.project.lift(self).map(_.testTwo)
  }

  def accept[A](visitor: TestIdRefUnion.Visitor[A]): A = this match {
    case value: TestIdRefUnion.TestCase => visitor.test(value.test)
    case value: TestIdRefUnion.TestTwoCase => visitor.testTwo(value.testTwo)
  }
}
object TestIdRefUnion extends ShapeTag.Companion[TestIdRefUnion] {

  def test(test: ShapeId): TestIdRefUnion = TestCase(test)
  def testTwo(testTwo: TestIdRefTwo): TestIdRefUnion = TestTwoCase(testTwo)

  val id: ShapeId = ShapeId("smithy4s.example", "TestIdRefUnion")

  val hints: Hints = Hints.empty

  final case class TestCase(test: ShapeId) extends TestIdRefUnion { final def $ordinal: Int = 0 }
  final case class TestTwoCase(testTwo: TestIdRefTwo) extends TestIdRefUnion { final def $ordinal: Int = 1 }

  object TestCase {
    val hints: Hints = Hints.empty
    val schema: Schema[TestIdRefUnion.TestCase] = bijection(string.refined[ShapeId](smithy.api.IdRef(selector = "*", failWhenMissing = None, errorMessage = None)).addHints(hints), TestIdRefUnion.TestCase(_), _.test)
    val alt = schema.oneOf[TestIdRefUnion]("test")
  }
  object TestTwoCase {
    val hints: Hints = Hints.empty
    val schema: Schema[TestIdRefUnion.TestTwoCase] = bijection(TestIdRefTwo.schema.addHints(hints), TestIdRefUnion.TestTwoCase(_), _.testTwo)
    val alt = schema.oneOf[TestIdRefUnion]("testTwo")
  }

  trait Visitor[A] {
    def test(value: ShapeId): A
    def testTwo(value: TestIdRefTwo): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def test(value: ShapeId): A = default
      def testTwo(value: TestIdRefTwo): A = default
    }
  }

  implicit val schema: Schema[TestIdRefUnion] = union(
    TestIdRefUnion.TestCase.alt,
    TestIdRefUnion.TestTwoCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
