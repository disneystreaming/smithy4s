package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.bijection
import _root_.smithy4s.schema.Schema.union
import smithy4s.schema.Schema.string

sealed trait TestIdRefUnion extends _root_.scala.Product with _root_.scala.Serializable { self =>
  @inline final def widen: TestIdRefUnion = this
  def $ordinal: Int

  object project {
    def test: Option[smithy4s.ShapeId] = TestIdRefUnion.TestCase.alt.project.lift(self).map(_.test)
    def testTwo: Option[TestIdRefTwo] = TestIdRefUnion.TestTwoCase.alt.project.lift(self).map(_.testTwo)
  }

  def accept[A](visitor: TestIdRefUnion.Visitor[A]): A = this match {
    case value: TestIdRefUnion.TestCase => visitor.test(value.test)
    case value: TestIdRefUnion.TestTwoCase => visitor.testTwo(value.testTwo)
  }
}
object TestIdRefUnion extends ShapeTag.Companion[TestIdRefUnion] {

  def test(test: smithy4s.ShapeId): TestIdRefUnion = TestCase(test)
  def testTwo(testTwo: TestIdRefTwo): TestIdRefUnion = TestTwoCase(testTwo)

  val id: _root_.smithy4s.ShapeId = _root_.smithy4s.ShapeId("smithy4s.example", "TestIdRefUnion")

  val hints: Hints = Hints.empty

  final case class TestCase(test: smithy4s.ShapeId) extends TestIdRefUnion { final def $ordinal: Int = 0 }
  final case class TestTwoCase(testTwo: TestIdRefTwo) extends TestIdRefUnion { final def $ordinal: Int = 1 }

  object TestCase {
    val hints: Hints = Hints.empty
    val schema: Schema[TestIdRefUnion.TestCase] = bijection(string.refined[smithy4s.ShapeId](smithy.api.IdRef(selector = "*", failWhenMissing = None, errorMessage = None)).addHints(hints), TestIdRefUnion.TestCase(_), _.test)
    val alt = schema.oneOf[TestIdRefUnion]("test")
  }
  object TestTwoCase {
    val hints: Hints = Hints.empty
    val schema: Schema[TestIdRefUnion.TestTwoCase] = bijection(TestIdRefTwo.schema.addHints(hints), TestIdRefUnion.TestTwoCase(_), _.testTwo)
    val alt = schema.oneOf[TestIdRefUnion]("testTwo")
  }

  trait Visitor[A] {
    def test(value: smithy4s.ShapeId): A
    def testTwo(value: TestIdRefTwo): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def test(value: smithy4s.ShapeId): A = default
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
