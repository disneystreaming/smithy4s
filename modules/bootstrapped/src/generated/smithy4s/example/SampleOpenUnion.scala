package smithy4s.example

import SampleOpenUnion.UCaseAlt
import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

sealed trait SampleOpenUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: SampleOpenUnion = this
  def $ordinal: Int

  object project {
    def str: Option[String] = SampleOpenUnion.StrCase.alt.project.lift(self).map(_.str)
    def u: Option[SampleOpenUnion.UCase.type] = UCaseAlt.project.lift(self)
    def unknown: Option[Document] = SampleOpenUnion.UnknownCase.alt.project.lift(self).map(_.unknown)
  }

  def accept[A](visitor: SampleOpenUnion.Visitor[A]): A = this match {
    case value: SampleOpenUnion.StrCase => visitor.str(value.str)
    case value: SampleOpenUnion.UCase.type => visitor.u(value)
    case value: SampleOpenUnion.UnknownCase => visitor.unknown(value.unknown)
  }
}
object SampleOpenUnion extends ShapeTag.Companion[SampleOpenUnion] {

  def str(str: String): SampleOpenUnion = StrCase(str)
  def u(): SampleOpenUnion = SampleOpenUnion.UCase
  def unknown(unknown: Document): SampleOpenUnion = UnknownCase(unknown)

  val id: ShapeId = ShapeId("smithy4s.example", "SampleOpenUnion")

  val hints: Hints = Hints.empty

  final case class StrCase(str: String) extends SampleOpenUnion { final def $ordinal: Int = 0 }
  case object UCase extends SampleOpenUnion { final def $ordinal: Int = 1 }
  private val UCaseAlt = Schema.constant(SampleOpenUnion.UCase).oneOf[SampleOpenUnion]("u").addHints(hints)
  final case class UnknownCase(unknown: Document) extends SampleOpenUnion { final def $ordinal: Int = 2 }

  object StrCase {
    val hints: Hints = Hints.empty
    val schema: Schema[SampleOpenUnion.StrCase] = bijection(string.addHints(hints), SampleOpenUnion.StrCase(_), _.str)
    val alt = schema.oneOf[SampleOpenUnion]("str")
  }
  object UnknownCase {
    val hints: Hints = Hints(
      alloy.JsonUnknown(),
    ).lazily
    val schema: Schema[SampleOpenUnion.UnknownCase] = bijection(document.addHints(hints), SampleOpenUnion.UnknownCase(_), _.unknown)
    val alt = schema.oneOf[SampleOpenUnion]("unknown")
  }

  trait Visitor[A] {
    def str(value: String): A
    def u(value: SampleOpenUnion.UCase.type): A
    def unknown(value: Document): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def str(value: String): A = default
      def u(value: SampleOpenUnion.UCase.type): A = default
      def unknown(value: Document): A = default
    }
  }

  implicit val schema: Schema[SampleOpenUnion] = union(
    SampleOpenUnion.StrCase.alt,
    UCaseAlt,
    SampleOpenUnion.UnknownCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
