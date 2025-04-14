package smithy4s.example

import RecursiveDiscriminatedOpenUnion.EndCaseAlt
import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.union

sealed trait RecursiveDiscriminatedOpenUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: RecursiveDiscriminatedOpenUnion = this
  def $ordinal: Int

  object project {
    def rec: Option[HasRecursiveDiscriminatedOpenUnion] = RecursiveDiscriminatedOpenUnion.RecCase.alt.project.lift(self).map(_.rec)
    def end: Option[RecursiveDiscriminatedOpenUnion.EndCase.type] = EndCaseAlt.project.lift(self)
    def unknown: Option[Document] = RecursiveDiscriminatedOpenUnion.UnknownCase.alt.project.lift(self).map(_.unknown)
  }

  def accept[A](visitor: RecursiveDiscriminatedOpenUnion.Visitor[A]): A = this match {
    case value: RecursiveDiscriminatedOpenUnion.RecCase => visitor.rec(value.rec)
    case value: RecursiveDiscriminatedOpenUnion.EndCase.type => visitor.end(value)
    case value: RecursiveDiscriminatedOpenUnion.UnknownCase => visitor.unknown(value.unknown)
  }
}
object RecursiveDiscriminatedOpenUnion extends ShapeTag.Companion[RecursiveDiscriminatedOpenUnion] {

  def rec(rec: HasRecursiveDiscriminatedOpenUnion): RecursiveDiscriminatedOpenUnion = RecCase(rec)
  def end(): RecursiveDiscriminatedOpenUnion = RecursiveDiscriminatedOpenUnion.EndCase
  def unknown(unknown: Document): RecursiveDiscriminatedOpenUnion = UnknownCase(unknown)

  val id: ShapeId = ShapeId("smithy4s.example", "RecursiveDiscriminatedOpenUnion")

  val hints: Hints = Hints(
    alloy.Discriminated("type"),
  ).lazily

  final case class RecCase(rec: HasRecursiveDiscriminatedOpenUnion) extends RecursiveDiscriminatedOpenUnion { final def $ordinal: Int = 0 }
  case object EndCase extends RecursiveDiscriminatedOpenUnion { final def $ordinal: Int = 1 }
  private val EndCaseAlt = Schema.constant(RecursiveDiscriminatedOpenUnion.EndCase).oneOf[RecursiveDiscriminatedOpenUnion]("end").addHints(hints)
  final case class UnknownCase(unknown: Document) extends RecursiveDiscriminatedOpenUnion { final def $ordinal: Int = 2 }

  object RecCase {
    val hints: Hints = Hints.empty
    val schema: Schema[RecursiveDiscriminatedOpenUnion.RecCase] = bijection(HasRecursiveDiscriminatedOpenUnion.schema.addHints(hints), RecursiveDiscriminatedOpenUnion.RecCase(_), _.rec)
    val alt = schema.oneOf[RecursiveDiscriminatedOpenUnion]("rec")
  }
  object UnknownCase {
    val hints: Hints = Hints(
      alloy.JsonUnknown(),
    ).lazily
    val schema: Schema[RecursiveDiscriminatedOpenUnion.UnknownCase] = bijection(document.addHints(hints), RecursiveDiscriminatedOpenUnion.UnknownCase(_), _.unknown)
    val alt = schema.oneOf[RecursiveDiscriminatedOpenUnion]("unknown")
  }

  trait Visitor[A] {
    def rec(value: HasRecursiveDiscriminatedOpenUnion): A
    def end(value: RecursiveDiscriminatedOpenUnion.EndCase.type): A
    def unknown(value: Document): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def rec(value: HasRecursiveDiscriminatedOpenUnion): A = default
      def end(value: RecursiveDiscriminatedOpenUnion.EndCase.type): A = default
      def unknown(value: Document): A = default
    }
  }

  implicit val schema: Schema[RecursiveDiscriminatedOpenUnion] = recursive(union(
    RecursiveDiscriminatedOpenUnion.RecCase.alt,
    EndCaseAlt,
    RecursiveDiscriminatedOpenUnion.UnknownCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints))
}
