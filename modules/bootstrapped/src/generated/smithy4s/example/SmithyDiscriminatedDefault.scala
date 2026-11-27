package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait SmithyDiscriminatedDefault extends scala.Product with scala.Serializable { self =>
  @inline final def widen: SmithyDiscriminatedDefault = this
  def $ordinal: Int

  object project {
    def one: Option[InlineCollectionsItem] = SmithyDiscriminatedDefault.OneCase.alt.project.lift(self).map(_.one)
    def two: Option[InlineCollections] = SmithyDiscriminatedDefault.TwoCase.alt.project.lift(self).map(_.two)
  }

  def accept[A](visitor: SmithyDiscriminatedDefault.Visitor[A]): A = this match {
    case value: SmithyDiscriminatedDefault.OneCase => visitor.one(value.one)
    case value: SmithyDiscriminatedDefault.TwoCase => visitor.two(value.two)
  }
}
object SmithyDiscriminatedDefault extends ShapeTag.Companion[SmithyDiscriminatedDefault] {

  def one(one: InlineCollectionsItem): SmithyDiscriminatedDefault = OneCase(one)
  def two(two: InlineCollections): SmithyDiscriminatedDefault = TwoCase(two)

  val id: ShapeId = ShapeId("smithy4s.example", "SmithyDiscriminatedDefault")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "discriminated"), smithy4s.Document.fromString("__type")),
  )

  final case class OneCase(one: InlineCollectionsItem) extends SmithyDiscriminatedDefault { final def $ordinal: Int = 0 }
  final case class TwoCase(two: InlineCollections) extends SmithyDiscriminatedDefault { final def $ordinal: Int = 1 }

  object OneCase {
    val hints: Hints = Hints.empty
    val schema: Schema[SmithyDiscriminatedDefault.OneCase] = bijection(InlineCollectionsItem.schema.addHints(hints), SmithyDiscriminatedDefault.OneCase(_), _.one)
    val alt = schema.oneOf[SmithyDiscriminatedDefault]("one")
  }
  object TwoCase {
    val hints: Hints = Hints.empty
    val schema: Schema[SmithyDiscriminatedDefault.TwoCase] = bijection(InlineCollections.schema.addHints(hints), SmithyDiscriminatedDefault.TwoCase(_), _.two)
    val alt = schema.oneOf[SmithyDiscriminatedDefault]("two")
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

  implicit val schema: Schema[SmithyDiscriminatedDefault] = union[SmithyDiscriminatedDefault](
    SmithyDiscriminatedDefault.OneCase.alt,
    SmithyDiscriminatedDefault.TwoCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
