package smithy4s.example

import RecursiveOpenUnion.EndCaseAlt
import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.union

sealed trait RecursiveOpenUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: RecursiveOpenUnion = this
  def $ordinal: Int

  object project {
    def rec: Option[smithy4s.example.RecursiveOpenUnion] = RecursiveOpenUnion.RecCase.alt.project.lift(self).map(_.rec)
    def end: Option[RecursiveOpenUnion.EndCase.type] = EndCaseAlt.project.lift(self)
    def unknown: Option[Document] = RecursiveOpenUnion.UnknownCase.alt.project.lift(self).map(_.unknown)
  }

  def accept[A](visitor: RecursiveOpenUnion.Visitor[A]): A = this match {
    case value: RecursiveOpenUnion.RecCase => visitor.rec(value.rec)
    case value: RecursiveOpenUnion.EndCase.type => visitor.end(value)
    case value: RecursiveOpenUnion.UnknownCase => visitor.unknown(value.unknown)
  }
}
object RecursiveOpenUnion extends ShapeTag.Companion[RecursiveOpenUnion] {

  def rec(rec: smithy4s.example.RecursiveOpenUnion): RecursiveOpenUnion = RecCase(rec)
  def end(): RecursiveOpenUnion = RecursiveOpenUnion.EndCase
  def unknown(unknown: Document): RecursiveOpenUnion = UnknownCase(unknown)

  val id: ShapeId = ShapeId("smithy4s.example", "RecursiveOpenUnion")

  val hints: Hints = Hints.empty

  final case class RecCase(rec: smithy4s.example.RecursiveOpenUnion) extends RecursiveOpenUnion { final def $ordinal: Int = 0 }
  case object EndCase extends RecursiveOpenUnion { final def $ordinal: Int = 1 }
  private val EndCaseAlt = Schema.constant(RecursiveOpenUnion.EndCase).oneOf[RecursiveOpenUnion]("end").addHints(hints)
  final case class UnknownCase(unknown: Document) extends RecursiveOpenUnion { final def $ordinal: Int = 2 }

  object RecCase {
    val hints: Hints = Hints.empty
    val schema: Schema[RecursiveOpenUnion.RecCase] = bijection(smithy4s.example.RecursiveOpenUnion.schema.addHints(hints), RecursiveOpenUnion.RecCase(_), _.rec)
    val alt = schema.oneOf[RecursiveOpenUnion]("rec")
  }
  object UnknownCase {
    val hints: Hints = Hints(
      alloy.JsonUnknown(),
    ).lazily
    val schema: Schema[RecursiveOpenUnion.UnknownCase] = bijection(document.addHints(hints), RecursiveOpenUnion.UnknownCase(_), _.unknown)
    val alt = schema.oneOf[RecursiveOpenUnion]("unknown")
  }

  trait Visitor[A] {
    def rec(value: smithy4s.example.RecursiveOpenUnion): A
    def end(value: RecursiveOpenUnion.EndCase.type): A
    def unknown(value: Document): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def rec(value: smithy4s.example.RecursiveOpenUnion): A = default
      def end(value: RecursiveOpenUnion.EndCase.type): A = default
      def unknown(value: Document): A = default
    }
  }

  implicit val schema: Schema[RecursiveOpenUnion] = recursive(union(
    RecursiveOpenUnion.RecCase.alt,
    EndCaseAlt,
    RecursiveOpenUnion.UnknownCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints))
}
