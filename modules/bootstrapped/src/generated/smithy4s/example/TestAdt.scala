package smithy4s.example

import smithy4s.ByteArray
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.bytes
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.short
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.union

sealed trait TestAdt extends AdtMixinOne with AdtMixinTwo with scala.Product with scala.Serializable {
  @inline final def widen: TestAdt = this
  def _ordinal: Int
}
object TestAdt extends ShapeTag.$Companion[TestAdt] {

  def adtOne(lng: Option[Long] = None, sht: Option[Short] = None, blb: Option[ByteArray] = None, str: Option[String] = None): TestAdt = AdtOne(lng, sht, blb, str)
  def adtTwo(lng: Option[Long] = None, sht: Option[Short] = None, int: Option[Int] = None): TestAdt = AdtTwo(lng, sht, int)

  val $id: ShapeId = ShapeId("smithy4s.example", "TestAdt")

  val $hints: Hints = Hints.empty

  final case class AdtOne(lng: Option[Long] = None, sht: Option[Short] = None, blb: Option[ByteArray] = None, str: Option[String] = None) extends TestAdt with AdtMixinThree {
    def _ordinal: Int = 0
  }
  object AdtOne extends ShapeTag.$Companion[AdtOne] {
    val $id: ShapeId = ShapeId("smithy4s.example", "AdtOne")

    val $hints: Hints = Hints.empty

    val lng: FieldLens[AdtOne, Option[Long]] = long.optional[AdtOne]("lng", _.lng, n => c => c.copy(lng = n))
    val sht: FieldLens[AdtOne, Option[Short]] = short.optional[AdtOne]("sht", _.sht, n => c => c.copy(sht = n))
    val blb: FieldLens[AdtOne, Option[ByteArray]] = bytes.optional[AdtOne]("blb", _.blb, n => c => c.copy(blb = n))
    val str: FieldLens[AdtOne, Option[String]] = string.optional[AdtOne]("str", _.str, n => c => c.copy(str = n))

    val $schema: Schema[AdtOne] = struct(
      lng,
      sht,
      blb,
      str,
    ){
      AdtOne.apply
    }.withId($id).addHints($hints)
  }
  final case class AdtTwo(lng: Option[Long] = None, sht: Option[Short] = None, int: Option[Int] = None) extends TestAdt {
    def _ordinal: Int = 1
  }
  object AdtTwo extends ShapeTag.$Companion[AdtTwo] {
    val $id: ShapeId = ShapeId("smithy4s.example", "AdtTwo")

    val $hints: Hints = Hints.empty

    val lng: FieldLens[AdtTwo, Option[Long]] = long.optional[AdtTwo]("lng", _.lng, n => c => c.copy(lng = n))
    val sht: FieldLens[AdtTwo, Option[Short]] = short.optional[AdtTwo]("sht", _.sht, n => c => c.copy(sht = n))
    val int: FieldLens[AdtTwo, Option[Int]] = smithy4s.schema.Schema.int.optional[AdtTwo]("int", _.int, n => c => c.copy(int = n))

    val $schema: Schema[AdtTwo] = struct(
      lng,
      sht,
      int,
    ){
      AdtTwo.apply
    }.withId($id).addHints($hints)
  }


  val one = AdtOne.$schema.oneOf[TestAdt]("one")
  val two = AdtTwo.$schema.oneOf[TestAdt]("two")

  implicit val $schema: Schema[TestAdt] = union(
    one,
    two,
  ){
    _._ordinal
  }.withId($id).addHints($hints)
}
