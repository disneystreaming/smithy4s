package smithy4s.example

import UnionTraitWithUnitCase.UCaseAlt
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

sealed trait UnionTraitWithUnitCase extends scala.Product with scala.Serializable { self =>
  @inline final def widen: UnionTraitWithUnitCase = this
  def $ordinal: Int

  object project {
    def u: Option[UnionTraitWithUnitCase.UCase.type] = UCaseAlt.project.lift(self)
    def s: Option[String] = UnionTraitWithUnitCase.SCase.alt.project.lift(self).map(_.s)
  }

  def accept[A](visitor: UnionTraitWithUnitCase.Visitor[A]): A = this match {
    case value: UnionTraitWithUnitCase.UCase.type => visitor.u(value)
    case value: UnionTraitWithUnitCase.SCase => visitor.s(value.s)
  }
}
object UnionTraitWithUnitCase extends ShapeTag.Companion[UnionTraitWithUnitCase] {

  def u(): UnionTraitWithUnitCase = UnionTraitWithUnitCase.UCase
  def s(s: String): UnionTraitWithUnitCase = SCase(s)

  val id: ShapeId = ShapeId("smithy4s.example", "unionTraitWithUnitCase")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  ).lazily

  case object UCase extends UnionTraitWithUnitCase { final def $ordinal: Int = 0 }
  private val UCaseAlt = Schema.constant(UnionTraitWithUnitCase.UCase).oneOf[UnionTraitWithUnitCase]("u").addHints(hints)
  final case class SCase(s: String) extends UnionTraitWithUnitCase { final def $ordinal: Int = 1 }

  object SCase {
    val hints: Hints = Hints.empty
    val schema: Schema[UnionTraitWithUnitCase.SCase] = bijection(string.addHints(hints), UnionTraitWithUnitCase.SCase(_), _.s)
    val alt = schema.oneOf[UnionTraitWithUnitCase]("s")
  }

  trait Visitor[A] {
    def u(value: UnionTraitWithUnitCase.UCase.type): A
    def s(value: String): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def u(value: UnionTraitWithUnitCase.UCase.type): A = default
      def s(value: String): A = default
    }
  }

  implicit val schema: Schema[UnionTraitWithUnitCase] = recursive(union(
    UCaseAlt,
    UnionTraitWithUnitCase.SCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints))
}
