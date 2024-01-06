package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.bijection
import _root_.smithy4s.schema.Schema.union

sealed trait TestBiggerUnion extends _root_.scala.Product with _root_.scala.Serializable { self =>
  @inline final def widen: TestBiggerUnion = this
  def $ordinal: Int

  object project {
    def one: Option[One] = TestBiggerUnion.OneCase.alt.project.lift(self).map(_.one)
    def two: Option[Two] = TestBiggerUnion.TwoCase.alt.project.lift(self).map(_.two)
  }

  def accept[A](visitor: TestBiggerUnion.Visitor[A]): A = this match {
    case value: TestBiggerUnion.OneCase => visitor.one(value.one)
    case value: TestBiggerUnion.TwoCase => visitor.two(value.two)
  }
}
object TestBiggerUnion extends ShapeTag.Companion[TestBiggerUnion] {

  def one(one: One): TestBiggerUnion = OneCase(one)
  def two(two: Two): TestBiggerUnion = TwoCase(two)

  val id: ShapeId = ShapeId("smithy4s.example", "TestBiggerUnion")

  val hints: Hints = Hints(
    alloy.Discriminated("tpe"),
  )

  final case class OneCase(one: One) extends TestBiggerUnion { final def $ordinal: Int = 0 }
  final case class TwoCase(two: Two) extends TestBiggerUnion { final def $ordinal: Int = 1 }

  object OneCase {
    val hints: Hints = Hints.empty
    val schema: Schema[TestBiggerUnion.OneCase] = bijection(One.schema.addHints(hints), TestBiggerUnion.OneCase(_), _.one)
    val alt = schema.oneOf[TestBiggerUnion]("one")
  }
  object TwoCase {
    val hints: Hints = Hints.empty
    val schema: Schema[TestBiggerUnion.TwoCase] = bijection(Two.schema.addHints(hints), TestBiggerUnion.TwoCase(_), _.two)
    val alt = schema.oneOf[TestBiggerUnion]("two")
  }

  trait Visitor[A] {
    def one(value: One): A
    def two(value: Two): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def one(value: One): A = default
      def two(value: Two): A = default
    }
  }

  implicit val schema: Schema[TestBiggerUnion] = union(
    TestBiggerUnion.OneCase.alt,
    TestBiggerUnion.TwoCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
