package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait ReservedKeywordTraitExampleUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: ReservedKeywordTraitExampleUnion = this
  def $ordinal: Int

  object project {
    def member: Option[String] = ReservedKeywordTraitExampleUnion.MemberCase.alt.project.lift(self).map(_.member)
  }

  def accept[A](visitor: ReservedKeywordTraitExampleUnion.Visitor[A]): A = this match {
    case value: ReservedKeywordTraitExampleUnion.MemberCase => visitor.member(value.member)
  }
}
object ReservedKeywordTraitExampleUnion extends ShapeTag.Companion[ReservedKeywordTraitExampleUnion] {

  def member(member: String): ReservedKeywordTraitExampleUnion = MemberCase(member)

  val id: ShapeId = ShapeId("smithy4s.example.collision", "ReservedKeywordTraitExampleUnion")

  val hints: Hints = Hints(
    smithy4s.example.collision.ReservedKeywordStructTrait(_implicit = smithy4s.example.collision.String("demo"), _package = Some(smithy4s.example.collision.Packagee(_class = Some(42)))),
  )

  final case class MemberCase(member: String) extends ReservedKeywordTraitExampleUnion { final def $ordinal: Int = 0 }

  object MemberCase {
    val hints: Hints = Hints(
      smithy4s.example.collision.ReservedKeywordStructTrait(_implicit = smithy4s.example.collision.String("demo"), _package = Some(smithy4s.example.collision.Packagee(_class = Some(42)))),
    )
    val schema: Schema[ReservedKeywordTraitExampleUnion.MemberCase] = bijection(String.schema.addHints(hints), ReservedKeywordTraitExampleUnion.MemberCase(_), _.member)
    val alt = schema.oneOf[ReservedKeywordTraitExampleUnion]("member")
  }

  trait Visitor[A] {
    def member(value: String): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def member(value: String): A = default
    }
  }

  implicit val schema: Schema[ReservedKeywordTraitExampleUnion] = union(
    ReservedKeywordTraitExampleUnion.MemberCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
