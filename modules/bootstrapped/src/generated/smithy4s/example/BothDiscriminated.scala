package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait BothDiscriminated extends scala.Product with scala.Serializable { self =>
  @inline final def widen: BothDiscriminated = this
  def $ordinal: Int

  object project {
    def one: Option[InlineCollectionsItem] = BothDiscriminated.OneCase.alt.project.lift(self).map(_.one)
    def two: Option[InlineCollections] = BothDiscriminated.TwoCase.alt.project.lift(self).map(_.two)
  }

  def accept[A](visitor: BothDiscriminated.Visitor[A]): A = this match {
    case value: BothDiscriminated.OneCase => visitor.one(value.one)
    case value: BothDiscriminated.TwoCase => visitor.two(value.two)
  }
}
object BothDiscriminated extends ShapeTag.Companion[BothDiscriminated] {

  def one(one: InlineCollectionsItem): BothDiscriminated = OneCase(one)
  def two(two: InlineCollections): BothDiscriminated = TwoCase(two)

  val id: ShapeId = ShapeId("smithy4s.example", "BothDiscriminated")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy", "discriminated"), smithy4s.Document.fromString("type")),
    Hints.dynamic(ShapeId("smithy.api", "discriminated"), smithy4s.Document.fromString("kind")),
  )

  final case class OneCase(one: InlineCollectionsItem) extends BothDiscriminated { final def $ordinal: Int = 0 }
  final case class TwoCase(two: InlineCollections) extends BothDiscriminated { final def $ordinal: Int = 1 }

  object OneCase {
    val hints: Hints = Hints.empty
    val schema: Schema[BothDiscriminated.OneCase] = bijection(InlineCollectionsItem.schema.addHints(hints), BothDiscriminated.OneCase(_), _.one)
    val alt = schema.oneOf[BothDiscriminated]("one")
  }
  object TwoCase {
    val hints: Hints = Hints.empty
    val schema: Schema[BothDiscriminated.TwoCase] = bijection(InlineCollections.schema.addHints(hints), BothDiscriminated.TwoCase(_), _.two)
    val alt = schema.oneOf[BothDiscriminated]("two")
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

  implicit val schema: Schema[BothDiscriminated] = union[BothDiscriminated](
    BothDiscriminated.OneCase.alt,
    BothDiscriminated.TwoCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
