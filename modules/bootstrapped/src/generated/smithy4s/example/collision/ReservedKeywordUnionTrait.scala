package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.union

sealed trait ReservedKeywordUnionTrait extends scala.Product with scala.Serializable { self =>
  @inline final def widen: ReservedKeywordUnionTrait = this
  def $ordinal: Int

  object project {
    def _package: Option[PackageUnion] = ReservedKeywordUnionTrait.PackageCase.alt.project.lift(self).map(_._package)
  }

  def accept[A](visitor: ReservedKeywordUnionTrait.Visitor[A]): A = this match {
    case value: ReservedKeywordUnionTrait.PackageCase => visitor._package(value._package)
  }
}
object ReservedKeywordUnionTrait extends ShapeTag.Companion[ReservedKeywordUnionTrait] {

  def _package(_package: PackageUnion): ReservedKeywordUnionTrait = PackageCase(_package)

  val id: ShapeId = ShapeId("smithy4s.example.collision", "reservedKeywordUnionTrait")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  ).lazily

  final case class PackageCase(_package: PackageUnion) extends ReservedKeywordUnionTrait { final def $ordinal: Int = 0 }

  object PackageCase {
    val hints: Hints = Hints.empty
    val schema: Schema[ReservedKeywordUnionTrait.PackageCase] = bijection(PackageUnion.schema.addHints(hints), ReservedKeywordUnionTrait.PackageCase(_), _._package)
    val alt = schema.oneOf[ReservedKeywordUnionTrait]("package")
  }

  trait Visitor[A] {
    def _package(value: PackageUnion): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def _package(value: PackageUnion): A = default
    }
  }

  implicit val schema: Schema[ReservedKeywordUnionTrait] = recursive(union(
    ReservedKeywordUnionTrait.PackageCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints))
}
