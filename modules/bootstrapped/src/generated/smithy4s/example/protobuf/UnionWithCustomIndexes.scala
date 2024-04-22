package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.union

sealed trait UnionWithCustomIndexes extends scala.Product with scala.Serializable { self =>
  @inline final def widen: UnionWithCustomIndexes = this
  def $ordinal: Int

  object project {
    def a: Option[Int] = UnionWithCustomIndexes.ACase.alt.project.lift(self).map(_.a)
    def b: Option[Int] = UnionWithCustomIndexes.BCase.alt.project.lift(self).map(_.b)
    def c: Option[Int] = UnionWithCustomIndexes.CCase.alt.project.lift(self).map(_.c)
  }

  def accept[A](visitor: UnionWithCustomIndexes.Visitor[A]): A = this match {
    case value: UnionWithCustomIndexes.ACase => visitor.a(value.a)
    case value: UnionWithCustomIndexes.BCase => visitor.b(value.b)
    case value: UnionWithCustomIndexes.CCase => visitor.c(value.c)
  }
}
object UnionWithCustomIndexes extends ShapeTag.Companion[UnionWithCustomIndexes] {

  def a(a: Int): UnionWithCustomIndexes = ACase(a)
  def b(b: Int): UnionWithCustomIndexes = BCase(b)
  def c(c: Int): UnionWithCustomIndexes = CCase(c)

  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "UnionWithCustomIndexes")

  val hints: Hints = Hints.empty

  final case class ACase(a: Int) extends UnionWithCustomIndexes { final def $ordinal: Int = 0 }
  final case class BCase(b: Int) extends UnionWithCustomIndexes { final def $ordinal: Int = 1 }
  final case class CCase(c: Int) extends UnionWithCustomIndexes { final def $ordinal: Int = 2 }

  object ACase {
    val hints: Hints = Hints(
      alloy.proto.ProtoIndex(3),
    ).lazily
    val schema: Schema[UnionWithCustomIndexes.ACase] = bijection(int.addHints(hints), UnionWithCustomIndexes.ACase(_), _.a)
    val alt = schema.oneOf[UnionWithCustomIndexes]("a")
  }
  object BCase {
    val hints: Hints = Hints(
      alloy.proto.ProtoIndex(2),
    ).lazily
    val schema: Schema[UnionWithCustomIndexes.BCase] = bijection(int.addHints(hints), UnionWithCustomIndexes.BCase(_), _.b)
    val alt = schema.oneOf[UnionWithCustomIndexes]("b")
  }
  object CCase {
    val hints: Hints = Hints(
      alloy.proto.ProtoIndex(1),
    ).lazily
    val schema: Schema[UnionWithCustomIndexes.CCase] = bijection(int.addHints(hints), UnionWithCustomIndexes.CCase(_), _.c)
    val alt = schema.oneOf[UnionWithCustomIndexes]("c")
  }

  trait Visitor[A] {
    def a(value: Int): A
    def b(value: Int): A
    def c(value: Int): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def a(value: Int): A = default
      def b(value: Int): A = default
      def c(value: Int): A = default
    }
  }

  implicit val schema: Schema[UnionWithCustomIndexes] = union(
    UnionWithCustomIndexes.ACase.alt,
    UnionWithCustomIndexes.BCase.alt,
    UnionWithCustomIndexes.CCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
