package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

sealed trait TestUnionPatternTarget extends scala.Product with scala.Serializable { self =>
  @inline final def widen: TestUnionPatternTarget = this
  def $ordinal: Int

  object project {
    def one: Option[String] = TestUnionPatternTarget.OneCase.alt.project.lift(self).map(_.one)
    def two: Option[Int] = TestUnionPatternTarget.TwoCase.alt.project.lift(self).map(_.two)
  }

  def accept[A](visitor: TestUnionPatternTarget.Visitor[A]): A = this match {
    case value: TestUnionPatternTarget.OneCase => visitor.one(value.one)
    case value: TestUnionPatternTarget.TwoCase => visitor.two(value.two)
  }
}
object TestUnionPatternTarget extends ShapeTag.Companion[TestUnionPatternTarget] {

  def one(one: String): TestUnionPatternTarget = OneCase(one)
  def two(two: Int): TestUnionPatternTarget = TwoCase(two)

  val id: ShapeId = ShapeId("smithy4s.example", "TestUnionPatternTarget")

  val hints: Hints = Hints.empty

  final case class OneCase(one: String) extends TestUnionPatternTarget { final def $ordinal: Int = 0 }
  final case class TwoCase(two: Int) extends TestUnionPatternTarget { final def $ordinal: Int = 1 }

  object OneCase {
    val hints: Hints = Hints.empty
    val schema: Schema[TestUnionPatternTarget.OneCase] = bijection(string.addHints(hints), TestUnionPatternTarget.OneCase(_), _.one)
    val alt = schema.oneOf[TestUnionPatternTarget]("one")
  }
  object TwoCase {
    val hints: Hints = Hints.empty
    val schema: Schema[TestUnionPatternTarget.TwoCase] = bijection(int.addHints(hints), TestUnionPatternTarget.TwoCase(_), _.two)
    val alt = schema.oneOf[TestUnionPatternTarget]("two")
  }

  trait Visitor[A] {
    def one(value: String): A
    def two(value: Int): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def one(value: String): A = default
      def two(value: Int): A = default
    }
  }

  implicit val schema: Schema[TestUnionPatternTarget] = union[TestUnionPatternTarget](
    TestUnionPatternTarget.OneCase.alt,
    TestUnionPatternTarget.TwoCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
