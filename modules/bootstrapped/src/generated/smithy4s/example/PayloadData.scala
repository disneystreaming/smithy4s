package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class PayloadData(testBiggerUnion: Option[TestBiggerUnion] = None)
object PayloadData extends ShapeTag.Companion[PayloadData] {

  val testBiggerUnion = TestBiggerUnion.schema.optional[PayloadData]("testBiggerUnion", _.testBiggerUnion, n => c => c.copy(testBiggerUnion = n))

  implicit val schema: Schema[PayloadData] = struct(
    testBiggerUnion,
  ){
    PayloadData.apply
  }
  .withId(ShapeId("smithy4s.example", "PayloadData"))
  .addHints(
    Hints.empty
  )
}
