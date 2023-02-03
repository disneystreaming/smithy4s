package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.short
import smithy4s.schema.Schema.struct

case class AdtTwo(lng: Option[Long] = None, sht: Option[Short] = None, int: Option[Int] = None) extends AdtMixinTwo with AdtMixinOne
object AdtTwo extends ShapeTag.Companion[AdtTwo] {
  val id: ShapeId = ShapeId("smithy4s.example", "AdtTwo")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[AdtTwo] = struct(
    long.optional[AdtTwo]("lng", _.lng),
    short.optional[AdtTwo]("sht", _.sht),
    int.optional[AdtTwo]("int", _.int),
  ){
    AdtTwo.apply
  }.withId(id).addHints(hints)
}