package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct

final case class PayloadData(testBiggerUnion: Option[TestBiggerUnion] = None)

object PayloadData extends ShapeTag.Companion[PayloadData] {
  val id: ShapeId = ShapeId("smithy4s.example", "PayloadData")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[PayloadData] = struct(
    TestBiggerUnion.schema.optional[PayloadData]("testBiggerUnion", _.testBiggerUnion),
  ){
    PayloadData.apply
  }.withId(id).addHints(hints)
}
