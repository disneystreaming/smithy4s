package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.union

sealed trait OnlyUnknownOpenUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: OnlyUnknownOpenUnion = this
  def $ordinal: Int

  object project {
    def unknown: Option[Document] = OnlyUnknownOpenUnion.UnknownCase.alt.project.lift(self).map(_.unknown)
  }

  def accept[A](visitor: OnlyUnknownOpenUnion.Visitor[A]): A = this match {
    case value: OnlyUnknownOpenUnion.UnknownCase => visitor.unknown(value.unknown)
  }
}
object OnlyUnknownOpenUnion extends ShapeTag.Companion[OnlyUnknownOpenUnion] {

  def unknown(unknown: Document): OnlyUnknownOpenUnion = UnknownCase(unknown)

  val id: ShapeId = ShapeId("smithy4s.example", "OnlyUnknownOpenUnion")

  val hints: Hints = Hints.empty

  final case class UnknownCase(unknown: Document) extends OnlyUnknownOpenUnion { final def $ordinal: Int = 0 }

  object UnknownCase {
    val hints: Hints = Hints(
      alloy.JsonUnknown(),
    ).lazily
    val schema: Schema[OnlyUnknownOpenUnion.UnknownCase] = bijection(document.addHints(hints), OnlyUnknownOpenUnion.UnknownCase(_), _.unknown)
    val alt = schema.oneOf[OnlyUnknownOpenUnion]("unknown")
  }

  trait Visitor[A] {
    def unknown(value: Document): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def unknown(value: Document): A = default
    }
  }

  implicit val schema: Schema[OnlyUnknownOpenUnion] = union(
    OnlyUnknownOpenUnion.UnknownCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
