package smithy4s.example

import SampleOpenDiscriminatedUnion.UCaseAlt
import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.union

sealed trait SampleOpenDiscriminatedUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: SampleOpenDiscriminatedUnion = this
  def $ordinal: Int

  object project {
    def s: Option[StructForDiscrimination] = SampleOpenDiscriminatedUnion.SCase.alt.project.lift(self).map(_.s)
    def u: Option[SampleOpenDiscriminatedUnion.UCase.type] = UCaseAlt.project.lift(self)
    def unknown: Option[Document] = SampleOpenDiscriminatedUnion.UnknownCase.alt.project.lift(self).map(_.unknown)
  }

  def accept[A](visitor: SampleOpenDiscriminatedUnion.Visitor[A]): A = this match {
    case value: SampleOpenDiscriminatedUnion.SCase => visitor.s(value.s)
    case value: SampleOpenDiscriminatedUnion.UCase.type => visitor.u(value)
    case value: SampleOpenDiscriminatedUnion.UnknownCase => visitor.unknown(value.unknown)
  }
}
object SampleOpenDiscriminatedUnion extends ShapeTag.Companion[SampleOpenDiscriminatedUnion] {

  def s(s: StructForDiscrimination): SampleOpenDiscriminatedUnion = SCase(s)
  def u(): SampleOpenDiscriminatedUnion = SampleOpenDiscriminatedUnion.UCase
  def unknown(unknown: Document): SampleOpenDiscriminatedUnion = UnknownCase(unknown)

  val id: ShapeId = ShapeId("smithy4s.example", "SampleOpenDiscriminatedUnion")

  val hints: Hints = Hints(
    alloy.Discriminated("type"),
  ).lazily

  final case class SCase(s: StructForDiscrimination) extends SampleOpenDiscriminatedUnion { final def $ordinal: Int = 0 }
  case object UCase extends SampleOpenDiscriminatedUnion { final def $ordinal: Int = 1 }
  private val UCaseAlt = Schema.constant(SampleOpenDiscriminatedUnion.UCase).oneOf[SampleOpenDiscriminatedUnion]("u").addHints(hints)
  final case class UnknownCase(unknown: Document) extends SampleOpenDiscriminatedUnion { final def $ordinal: Int = 2 }

  object SCase {
    val hints: Hints = Hints.empty
    val schema: Schema[SampleOpenDiscriminatedUnion.SCase] = bijection(StructForDiscrimination.schema.addHints(hints), SampleOpenDiscriminatedUnion.SCase(_), _.s)
    val alt = schema.oneOf[SampleOpenDiscriminatedUnion]("s")
  }
  object UnknownCase {
    val hints: Hints = Hints(
      alloy.JsonUnknown(),
    ).lazily
    val schema: Schema[SampleOpenDiscriminatedUnion.UnknownCase] = bijection(document.addHints(hints), SampleOpenDiscriminatedUnion.UnknownCase(_), _.unknown)
    val alt = schema.oneOf[SampleOpenDiscriminatedUnion]("unknown")
  }

  trait Visitor[A] {
    def s(value: StructForDiscrimination): A
    def u(value: SampleOpenDiscriminatedUnion.UCase.type): A
    def unknown(value: Document): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def s(value: StructForDiscrimination): A = default
      def u(value: SampleOpenDiscriminatedUnion.UCase.type): A = default
      def unknown(value: Document): A = default
    }
  }

  implicit val schema: Schema[SampleOpenDiscriminatedUnion] = union(
    SampleOpenDiscriminatedUnion.SCase.alt,
    UCaseAlt,
    SampleOpenDiscriminatedUnion.UnknownCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
