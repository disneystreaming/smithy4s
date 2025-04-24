package smithy4s.example.protobuf

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
    def list: Option[List[MyInt]] = MyUnion.ListCase.alt.project.lift(self).map(_.list)
    def map: Option[Map[String, Int]] = MyUnion.MapCase.alt.project.lift(self).map(_.map)
  }

  def accept[A](visitor: MyUnion.Visitor[A]): A = this match {
    case value: MyUnion.IntCase => visitor.int(value.int)
    case value: MyUnion.BoolCase => visitor.bool(value.bool)
    case value: MyUnion.ListCase => visitor.list(value.list)
    case value: MyUnion.MapCase => visitor.map(value.map)
  }
}
object MyUnion extends ShapeTag.Companion[MyUnion] {

  def int(int: Int): MyUnion = IntCase(int)
  def bool(bool: Boolean): MyUnion = BoolCase(bool)
  def list(list: List[MyInt]): MyUnion = ListCase(list)
  def map(map: Map[String, Int]): MyUnion = MapCase(map)

  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "MyUnion")

  val hints: Hints = Hints.empty

  final case class IntCase(int: Int) extends MyUnion { final def $ordinal: Int = 0 }
  final case class BoolCase(bool: Boolean) extends MyUnion { final def $ordinal: Int = 1 }
  final case class ListCase(list: List[MyInt]) extends MyUnion { final def $ordinal: Int = 2 }
  final case class MapCase(map: Map[String, Int]) extends MyUnion { final def $ordinal: Int = 3 }

  object IntCase {
    val hints: Hints = Hints.empty
    val schema: Schema[MyUnion.IntCase] = bijection(smithy4s.schema.Schema.int.addHints(hints), MyUnion.IntCase(_), _.int)
    val alt = schema.oneOf[MyUnion]("int")
  }
  object BoolCase {
    val hints: Hints = Hints.empty
    val schema: Schema[MyUnion.BoolCase] = bijection(boolean.addHints(hints), MyUnion.BoolCase(_), _.bool)
    val alt = schema.oneOf[MyUnion]("bool")
  }
  object ListCase {
    val hints: Hints = Hints(
      alloy.proto.ProtoWrapped(),
    ).lazily
    val schema: Schema[MyUnion.ListCase] = bijection(MyIntList.underlyingSchema.addHints(hints), MyUnion.ListCase(_), _.list)
    val alt = schema.oneOf[MyUnion]("list")
  }
  object MapCase {
    val hints: Hints = Hints(
      alloy.proto.ProtoWrapped(),
    ).lazily
    val schema: Schema[MyUnion.MapCase] = bijection(StringMap.underlyingSchema.addHints(hints), MyUnion.MapCase(_), _.map)
    val alt = schema.oneOf[MyUnion]("map")
  }

  trait Visitor[A] {
    def int(value: Int): A
    def bool(value: Boolean): A
    def list(value: List[MyInt]): A
    def map(value: Map[String, Int]): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def int(value: Int): A = default
      def bool(value: Boolean): A = default
      def list(value: List[MyInt]): A = default
      def map(value: Map[String, Int]): A = default
    }
  }

  implicit val schema: Schema[MyUnion] = union(
    MyUnion.IntCase.alt,
    MyUnion.BoolCase.alt,
    MyUnion.ListCase.alt,
    MyUnion.MapCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
