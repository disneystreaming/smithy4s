package smithy4s.example

import smithy4s.Blob
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bytes
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.short
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.union

sealed trait TestAdt extends AdtMixinOne with AdtMixinTwo with scala.Product with scala.Serializable { self =>
  @inline final def widen: TestAdt = this
  def $ordinal: Int

  object project {
    def one: Option[TestAdt.AdtOne] = TestAdt.AdtOne.alt.project.lift(self)
    def two: Option[TestAdt.AdtTwo] = TestAdt.AdtTwo.alt.project.lift(self)
  }

  def accept[A](visitor: TestAdt.Visitor[A]): A = this match {
    case value: TestAdt.AdtOne => visitor.one(value)
    case value: TestAdt.AdtTwo => visitor.two(value)
  }
}
object TestAdt extends ShapeTag.Companion[TestAdt] {

  def adtOne(lng: Option[Long] = None, sht: Option[Short] = None, blb: Option[Blob] = None, str: Option[String] = None):AdtOne = AdtOne(lng, sht, blb, str)
  def adtTwo(lng: Option[Long] = None, sht: Option[Short] = None, int: Option[Int] = None):AdtTwo = AdtTwo(lng, sht, int)

  val id: ShapeId = ShapeId("smithy4s.example", "TestAdt")

  val hints: Hints = Hints.empty

  final case class AdtOne(lng: Option[Long] = None, sht: Option[Short] = None, blb: Option[Blob] = None, str: Option[String] = None) extends TestAdt with AdtMixinThree {
    def $ordinal: Int = 0
  }
  object AdtOne extends ShapeTag.Companion[AdtOne] {
    val id: ShapeId = ShapeId("smithy4s.example", "AdtOne")

    val hints: Hints = Hints.empty

    val schema: Schema[AdtOne] = struct(
      long.optional[AdtOne]("lng", _.lng),
      short.optional[AdtOne]("sht", _.sht),
      bytes.optional[AdtOne]("blb", _.blb),
      string.optional[AdtOne]("str", _.str),
    ){
      AdtOne.apply
    }.withId(id).addHints(hints)

    val alt = schema.oneOf[TestAdt]("one")
  }
  final case class AdtTwo(lng: Option[Long] = None, sht: Option[Short] = None, int: Option[Int] = None) extends TestAdt {
    def $ordinal: Int = 1
  }
  object AdtTwo extends ShapeTag.Companion[AdtTwo] {
    val id: ShapeId = ShapeId("smithy4s.example", "AdtTwo")

    val hints: Hints = Hints.empty

    val schema: Schema[AdtTwo] = struct(
      long.optional[AdtTwo]("lng", _.lng),
      short.optional[AdtTwo]("sht", _.sht),
      int.optional[AdtTwo]("int", _.int),
    ){
      AdtTwo.apply
    }.withId(id).addHints(hints)

    val alt = schema.oneOf[TestAdt]("two")
  }


  trait Visitor[A] {
    def one(value: TestAdt.AdtOne): A
    def two(value: TestAdt.AdtTwo): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def one(value: TestAdt.AdtOne): A = default
      def two(value: TestAdt.AdtTwo): A = default
    }
  }

  implicit val schema: Schema[TestAdt] = union(
    TestAdt.AdtOne.alt,
    TestAdt.AdtTwo.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
