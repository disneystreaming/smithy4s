package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.union

sealed trait OnlyUnknownDiscriminatedOpenUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: OnlyUnknownDiscriminatedOpenUnion = this
  def $ordinal: Int

  object project {
    def unknown: Option[Document] = OnlyUnknownDiscriminatedOpenUnion.UnknownCase.alt.project.lift(self).map(_.unknown)
  }

  def accept[A](visitor: OnlyUnknownDiscriminatedOpenUnion.Visitor[A]): A = this match {
    case value: OnlyUnknownDiscriminatedOpenUnion.UnknownCase => visitor.unknown(value.unknown)
  }
}
object OnlyUnknownDiscriminatedOpenUnion extends ShapeTag.Companion[OnlyUnknownDiscriminatedOpenUnion] {

  def unknown(unknown: Document): OnlyUnknownDiscriminatedOpenUnion = UnknownCase(unknown)

  val id: ShapeId = ShapeId("smithy4s.example", "OnlyUnknownDiscriminatedOpenUnion")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy", "discriminated"), smithy4s.Document.fromString("type")),
  )

  final case class UnknownCase(unknown: Document) extends OnlyUnknownDiscriminatedOpenUnion { final def $ordinal: Int = 0 }

  object UnknownCase {
    val hints: Hints = Hints(
      Hints.dynamic(ShapeId("alloy", "jsonUnknown"), smithy4s.Document.obj()),
    )
    val schema: Schema[OnlyUnknownDiscriminatedOpenUnion.UnknownCase] = bijection(document.addHints(hints), OnlyUnknownDiscriminatedOpenUnion.UnknownCase(_), _.unknown)
    val alt = schema.oneOf[OnlyUnknownDiscriminatedOpenUnion]("unknown")
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

  implicit val schema: Schema[OnlyUnknownDiscriminatedOpenUnion] = union(
    OnlyUnknownDiscriminatedOpenUnion.UnknownCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
