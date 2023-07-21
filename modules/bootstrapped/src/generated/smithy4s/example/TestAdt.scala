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

sealed trait TestAdt extends AdtMixinOne with AdtMixinTwo with scala.Product with scala.Serializable {
  @inline final def widen: TestAdt = this
}
object TestAdt extends ShapeTag.Companion[TestAdt] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestAdt")

  val hints: Hints = Hints.empty

  final case class AdtOne(lng: Option[Long] = None, sht: Option[Short] = None, blb: Option[Blob] = None, str: Option[String] = None) extends TestAdt with AdtMixinThree
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
  final case class AdtTwo(lng: Option[Long] = None, sht: Option[Short] = None, int: Option[Int] = None) extends TestAdt
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


  implicit val schema: Schema[TestAdt] = union(
    AdtOne.alt,
    AdtTwo.alt,
  ){
    case c: AdtOne => AdtOne.alt(c)
    case c: AdtTwo => AdtTwo.alt(c)
  }.withId(id).addHints(hints)
}
