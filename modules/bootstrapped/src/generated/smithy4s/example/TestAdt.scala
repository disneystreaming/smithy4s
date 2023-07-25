package smithy4s.example

import smithy4s.ByteArray
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

sealed trait TestAdt extends AdtMixinOne with AdtMixinTwo with scala.Product with scala.Serializable {
  @inline final def widen: TestAdt = this
  def _ordinal: Int
}
object TestAdt extends ShapeTag.Companion[TestAdt] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestAdt")

  val hints: Hints = Hints.empty

  final case class AdtOne(lng: Option[Long] = None, sht: Option[Short] = None, blb: Option[ByteArray] = None, str: Option[String] = None) extends TestAdt with AdtMixinThree {
    def _ordinal: Int = 0
  }
  object AdtOne extends ShapeTag.Companion[AdtOne] {
    val id: ShapeId = ShapeId("smithy4s.example", "AdtOne")

    val hints: Hints = Hints.empty

    val lng = long.optional[AdtOne]("lng", _.lng)
    val sht = short.optional[AdtOne]("sht", _.sht)
    val blb = bytes.optional[AdtOne]("blb", _.blb)
    val str = string.optional[AdtOne]("str", _.str)

    val schema: Schema[AdtOne] = struct(
      lng,
      sht,
      blb,
      str,
    ){
      AdtOne.apply
    }.withId(id).addHints(hints)

    val alt = schema.oneOf[TestAdt]("one")
  }
  final case class AdtTwo(lng: Option[Long] = None, sht: Option[Short] = None, int: Option[Int] = None) extends TestAdt {
    def _ordinal: Int = 1
  }
  object AdtTwo extends ShapeTag.Companion[AdtTwo] {
    val id: ShapeId = ShapeId("smithy4s.example", "AdtTwo")

    val hints: Hints = Hints.empty

    val lng = long.optional[AdtTwo]("lng", _.lng)
    val sht = short.optional[AdtTwo]("sht", _.sht)
    val int = int.optional[AdtTwo]("int", _.int)

    val schema: Schema[AdtTwo] = struct(
      lng,
      sht,
      int,
    ){
      AdtTwo.apply
    }.withId(id).addHints(hints)

    val alt = schema.oneOf[TestAdt]("two")
  }


  implicit val schema: Schema[TestAdt] = union(
    AdtOne.alt,
    AdtTwo.alt,
  ){
    _._ordinal
  }.withId(id).addHints(hints)
}
