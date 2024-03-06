package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.union

sealed trait MyUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: MyUnion = this
  def $ordinal: Int

  object project {
    def int: Option[Int] = MyUnion.IntCase.alt.project.lift(self).map(_.int)
    def bool: Option[Boolean] = MyUnion.BoolCase.alt.project.lift(self).map(_.bool)
  }

  def accept[A](visitor: MyUnion.Visitor[A]): A = this match {
    case value: MyUnion.IntCase => visitor.int(value.int)
    case value: MyUnion.BoolCase => visitor.bool(value.bool)
  }
}
object MyUnion extends ShapeTag.Companion[MyUnion] {

  def int(int: Int): MyUnion = IntCase(int)
  def bool(bool: Boolean): MyUnion = BoolCase(bool)

  val id: ShapeId = ShapeId("smithy4s.example", "MyUnion")

  val hints: Hints = Hints.empty

  final case class IntCase(int: Int) extends MyUnion { final def $ordinal: Int = 0 }
  final case class BoolCase(bool: Boolean) extends MyUnion { final def $ordinal: Int = 1 }

  object IntCase {
    val hints: Hints = Hints(
      alloy.proto.ProtoIndex(1),
    ).lazily
    val schema: Schema[MyUnion.IntCase] = bijection(smithy4s.schema.Schema.int.addHints(hints), MyUnion.IntCase(_), _.int)
    val alt = schema.oneOf[MyUnion]("int")
  }
  object BoolCase {
    val hints: Hints = Hints(
      alloy.proto.ProtoIndex(2),
    ).lazily
    val schema: Schema[MyUnion.BoolCase] = bijection(boolean.addHints(hints), MyUnion.BoolCase(_), _.bool)
    val alt = schema.oneOf[MyUnion]("bool")
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

  implicit val schema: Schema[MyUnion] = union(
    MyUnion.IntCase.alt,
    MyUnion.BoolCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
