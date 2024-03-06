package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.union

sealed trait MyInlinedUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: MyInlinedUnion = this
  def $ordinal: Int

  object project {
    def int: Option[Int] = MyInlinedUnion.IntCase.alt.project.lift(self).map(_.int)
    def bool: Option[Boolean] = MyInlinedUnion.BoolCase.alt.project.lift(self).map(_.bool)
  }

  def accept[A](visitor: MyInlinedUnion.Visitor[A]): A = this match {
    case value: MyInlinedUnion.IntCase => visitor.int(value.int)
    case value: MyInlinedUnion.BoolCase => visitor.bool(value.bool)
  }
}
object MyInlinedUnion extends ShapeTag.Companion[MyInlinedUnion] {

  def int(int: Int): MyInlinedUnion = IntCase(int)
  def bool(bool: Boolean): MyInlinedUnion = BoolCase(bool)

  val id: ShapeId = ShapeId("smithy4s.example", "MyInlinedUnion")

  val hints: Hints = Hints(
    alloy.proto.ProtoInlinedOneOf(),
  ).lazily

  final case class IntCase(int: Int) extends MyInlinedUnion { final def $ordinal: Int = 0 }
  final case class BoolCase(bool: Boolean) extends MyInlinedUnion { final def $ordinal: Int = 1 }

  object IntCase {
    val hints: Hints = Hints(
      alloy.proto.ProtoIndex(1),
    ).lazily
    val schema: Schema[MyInlinedUnion.IntCase] = bijection(smithy4s.schema.Schema.int.addHints(hints), MyInlinedUnion.IntCase(_), _.int)
    val alt = schema.oneOf[MyInlinedUnion]("int")
  }
  object BoolCase {
    val hints: Hints = Hints(
      alloy.proto.ProtoIndex(2),
    ).lazily
    val schema: Schema[MyInlinedUnion.BoolCase] = bijection(boolean.addHints(hints), MyInlinedUnion.BoolCase(_), _.bool)
    val alt = schema.oneOf[MyInlinedUnion]("bool")
  }

  trait Visitor[A] {
    def int(value: Int): A
    def bool(value: Boolean): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def int(value: Int): A = default
      def bool(value: Boolean): A = default
    }
  }

  implicit val schema: Schema[MyInlinedUnion] = union(
    MyInlinedUnion.IntCase.alt,
    MyInlinedUnion.BoolCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
