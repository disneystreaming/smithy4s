package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait SmithyDiscriminated extends scala.Product with scala.Serializable { self =>
  @inline final def widen: SmithyDiscriminated = this
  def $ordinal: Int

  object project {
    def one: Option[InlineCollectionsItem] = SmithyDiscriminated.OneCase.alt.project.lift(self).map(_.one)
    def two: Option[InlineCollections] = SmithyDiscriminated.TwoCase.alt.project.lift(self).map(_.two)
  }

  def accept[A](visitor: SmithyDiscriminated.Visitor[A]): A = this match {
    case value: SmithyDiscriminated.OneCase => visitor.one(value.one)
    case value: SmithyDiscriminated.TwoCase => visitor.two(value.two)
  }
}
object SmithyDiscriminated extends ShapeTag.Companion[SmithyDiscriminated] {

  def one(one: InlineCollectionsItem): SmithyDiscriminated = OneCase(one)
  def two(two: InlineCollections): SmithyDiscriminated = TwoCase(two)

  val id: ShapeId = ShapeId("smithy4s.example", "SmithyDiscriminated")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "discriminated"), smithy4s.Document.fromString("kind")),
  )

  final case class OneCase(one: InlineCollectionsItem) extends SmithyDiscriminated { final def $ordinal: Int = 0 }
  final case class TwoCase(two: InlineCollections) extends SmithyDiscriminated { final def $ordinal: Int = 1 }

  object OneCase {
    val hints: Hints = Hints.empty
    val schema: Schema[SmithyDiscriminated.OneCase] = bijection(InlineCollectionsItem.schema.addHints(hints), SmithyDiscriminated.OneCase(_), _.one)
    val alt = schema.oneOf[SmithyDiscriminated]("one")
  }
  object TwoCase {
    val hints: Hints = Hints.empty
    val schema: Schema[SmithyDiscriminated.TwoCase] = bijection(InlineCollections.schema.addHints(hints), SmithyDiscriminated.TwoCase(_), _.two)
    val alt = schema.oneOf[SmithyDiscriminated]("two")
  }

  trait Visitor[A] {
    def one(value: InlineCollectionsItem): A
    def two(value: InlineCollections): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def one(value: InlineCollectionsItem): A = default
      def two(value: InlineCollections): A = default
    }
  }

  implicit val schema: Schema[SmithyDiscriminated] = union[SmithyDiscriminated](
    SmithyDiscriminated.OneCase.alt,
    SmithyDiscriminated.TwoCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
