package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.union

sealed trait AdtUnionWithTransitiveAndDirectMixins extends AdtMixinOne with TransitiveMixin with scala.Product with scala.Serializable { self =>
  @inline final def widen: AdtUnionWithTransitiveAndDirectMixins = this
  def $ordinal: Int

  object project {
    def s1: Option[AdtUnionWithTransitiveAndDirectMixins.AdtMemberWithTransitiveMixin2] = AdtUnionWithTransitiveAndDirectMixins.AdtMemberWithTransitiveMixin2.alt.project.lift(self)
    def s2: Option[AdtUnionWithTransitiveAndDirectMixins.AdtMemberWithTransitiveMixin3] = AdtUnionWithTransitiveAndDirectMixins.AdtMemberWithTransitiveMixin3.alt.project.lift(self)
  }

  def accept[A](visitor: AdtUnionWithTransitiveAndDirectMixins.Visitor[A]): A = this match {
    case value: AdtUnionWithTransitiveAndDirectMixins.AdtMemberWithTransitiveMixin2 => visitor.s1(value)
    case value: AdtUnionWithTransitiveAndDirectMixins.AdtMemberWithTransitiveMixin3 => visitor.s2(value)
  }
}
object AdtUnionWithTransitiveAndDirectMixins extends ShapeTag.Companion[AdtUnionWithTransitiveAndDirectMixins] {

  def adtMemberWithTransitiveMixin2(lng: Option[Long] = None): AdtMemberWithTransitiveMixin2 = AdtMemberWithTransitiveMixin2(lng)
  def adtMemberWithTransitiveMixin3(lng: Option[Long] = None): AdtMemberWithTransitiveMixin3 = AdtMemberWithTransitiveMixin3(lng)

  val id: ShapeId = ShapeId("smithy4s.example", "AdtUnionWithTransitiveAndDirectMixins")

  val hints: Hints = Hints.empty

  final case class AdtMemberWithTransitiveMixin2(lng: Option[Long] = None) extends AdtUnionWithTransitiveAndDirectMixins {
    def $ordinal: Int = 0
  }

  object AdtMemberWithTransitiveMixin2 {
    val id: ShapeId = ShapeId("smithy4s.example", "AdtMemberWithTransitiveMixin2")

    val hints: Hints = Hints.empty

    // constructor using the original order from the spec
    private def make(lng: Option[Long]): AdtMemberWithTransitiveMixin2 = AdtMemberWithTransitiveMixin2(lng)

    val schema: Schema[AdtMemberWithTransitiveMixin2] = struct(
      long.optional[AdtMemberWithTransitiveMixin2]("lng", _.lng),
    )(make).withId(id).addHints(hints)

    val alt = schema.oneOf[AdtUnionWithTransitiveAndDirectMixins]("s1")
  }
  final case class AdtMemberWithTransitiveMixin3(lng: Option[Long] = None) extends AdtUnionWithTransitiveAndDirectMixins {
    def $ordinal: Int = 1
  }

  object AdtMemberWithTransitiveMixin3 {
    val id: ShapeId = ShapeId("smithy4s.example", "AdtMemberWithTransitiveMixin3")

    val hints: Hints = Hints.empty

    // constructor using the original order from the spec
    private def make(lng: Option[Long]): AdtMemberWithTransitiveMixin3 = AdtMemberWithTransitiveMixin3(lng)

    val schema: Schema[AdtMemberWithTransitiveMixin3] = struct(
      long.optional[AdtMemberWithTransitiveMixin3]("lng", _.lng),
    )(make).withId(id).addHints(hints)

    val alt = schema.oneOf[AdtUnionWithTransitiveAndDirectMixins]("s2")
  }


  trait Visitor[A] {
    def s1(value: AdtUnionWithTransitiveAndDirectMixins.AdtMemberWithTransitiveMixin2): A
    def s2(value: AdtUnionWithTransitiveAndDirectMixins.AdtMemberWithTransitiveMixin3): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def s1(value: AdtUnionWithTransitiveAndDirectMixins.AdtMemberWithTransitiveMixin2): A = default
      def s2(value: AdtUnionWithTransitiveAndDirectMixins.AdtMemberWithTransitiveMixin3): A = default
    }
  }

  implicit val schema: Schema[AdtUnionWithTransitiveAndDirectMixins] = union(
    AdtUnionWithTransitiveAndDirectMixins.AdtMemberWithTransitiveMixin2.alt,
    AdtUnionWithTransitiveAndDirectMixins.AdtMemberWithTransitiveMixin3.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
