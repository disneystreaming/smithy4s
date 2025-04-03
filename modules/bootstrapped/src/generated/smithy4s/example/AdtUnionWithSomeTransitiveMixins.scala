package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.union

sealed trait AdtUnionWithSomeTransitiveMixins extends AdtMixinOne with scala.Product with scala.Serializable { self =>
  @inline final def widen: AdtUnionWithSomeTransitiveMixins = this
  def $ordinal: Int

  object project {
    def s1: Option[AdtUnionWithSomeTransitiveMixins.AdtMemberWithTransitiveMixin1] = AdtUnionWithSomeTransitiveMixins.AdtMemberWithTransitiveMixin1.alt.project.lift(self)
    def s2: Option[AdtUnionWithSomeTransitiveMixins.AdtMemberWithDirectMixin] = AdtUnionWithSomeTransitiveMixins.AdtMemberWithDirectMixin.alt.project.lift(self)
  }

  def accept[A](visitor: AdtUnionWithSomeTransitiveMixins.Visitor[A]): A = this match {
    case value: AdtUnionWithSomeTransitiveMixins.AdtMemberWithTransitiveMixin1 => visitor.s1(value)
    case value: AdtUnionWithSomeTransitiveMixins.AdtMemberWithDirectMixin => visitor.s2(value)
  }
}
object AdtUnionWithSomeTransitiveMixins extends ShapeTag.Companion[AdtUnionWithSomeTransitiveMixins] {

  def adtMemberWithTransitiveMixin1(lng: Option[Long] = None): AdtMemberWithTransitiveMixin1 = AdtMemberWithTransitiveMixin1(lng)
  def adtMemberWithDirectMixin(lng: Option[Long] = None): AdtMemberWithDirectMixin = AdtMemberWithDirectMixin(lng)

  val id: ShapeId = ShapeId("smithy4s.example", "AdtUnionWithSomeTransitiveMixins")

  val hints: Hints = Hints.empty

  final case class AdtMemberWithTransitiveMixin1(lng: Option[Long] = None) extends AdtUnionWithSomeTransitiveMixins with TransitiveMixin {
    def $ordinal: Int = 0
  }

  object AdtMemberWithTransitiveMixin1 {
    val id: ShapeId = ShapeId("smithy4s.example", "AdtMemberWithTransitiveMixin1")

    val hints: Hints = Hints.empty

    // constructor using the original order from the spec
    private def make(lng: Option[Long]): AdtMemberWithTransitiveMixin1 = AdtMemberWithTransitiveMixin1(lng)

    val schema: Schema[AdtMemberWithTransitiveMixin1] = struct(
      long.optional[AdtMemberWithTransitiveMixin1]("lng", _.lng),
    )(make).withId(id).addHints(hints)

    val alt = schema.oneOf[AdtUnionWithSomeTransitiveMixins]("s1")
  }
  final case class AdtMemberWithDirectMixin(lng: Option[Long] = None) extends AdtUnionWithSomeTransitiveMixins {
    def $ordinal: Int = 1
  }

  object AdtMemberWithDirectMixin {
    val id: ShapeId = ShapeId("smithy4s.example", "AdtMemberWithDirectMixin")

    val hints: Hints = Hints.empty

    // constructor using the original order from the spec
    private def make(lng: Option[Long]): AdtMemberWithDirectMixin = AdtMemberWithDirectMixin(lng)

    val schema: Schema[AdtMemberWithDirectMixin] = struct(
      long.optional[AdtMemberWithDirectMixin]("lng", _.lng),
    )(make).withId(id).addHints(hints)

    val alt = schema.oneOf[AdtUnionWithSomeTransitiveMixins]("s2")
  }


  trait Visitor[A] {
    def s1(value: AdtUnionWithSomeTransitiveMixins.AdtMemberWithTransitiveMixin1): A
    def s2(value: AdtUnionWithSomeTransitiveMixins.AdtMemberWithDirectMixin): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def s1(value: AdtUnionWithSomeTransitiveMixins.AdtMemberWithTransitiveMixin1): A = default
      def s2(value: AdtUnionWithSomeTransitiveMixins.AdtMemberWithDirectMixin): A = default
    }
  }

  implicit val schema: Schema[AdtUnionWithSomeTransitiveMixins] = union(
    AdtUnionWithSomeTransitiveMixins.AdtMemberWithTransitiveMixin1.alt,
    AdtUnionWithSomeTransitiveMixins.AdtMemberWithDirectMixin.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
