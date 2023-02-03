package smithy4s.example

import smithy4s.ByteArray
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bytes
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.short
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

case class AdtOne(lng: Option[Long] = None, sht: Option[Short] = None, blb: Option[ByteArray] = None, str: Option[String] = None) extends AdtMixinTwo with AdtMixinOne with AdtMixinThree
object AdtOne extends ShapeTag.Companion[AdtOne] {
  val id: ShapeId = ShapeId("smithy4s.example", "AdtOne")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[AdtOne] = struct(
    long.optional[AdtOne]("lng", _.lng),
    short.optional[AdtOne]("sht", _.sht),
    bytes.optional[AdtOne]("blb", _.blb),
    string.optional[AdtOne]("str", _.str),
  ){
    AdtOne.apply
  }.withId(id).addHints(hints)
}