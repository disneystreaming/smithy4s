package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct

final case class PayloadData(testBiggerUnion: Option[TestBiggerUnion] = None)
object PayloadData extends ShapeTag.$Companion[PayloadData] {
  val $id: ShapeId = ShapeId("smithy4s.example", "PayloadData")

  val $hints: Hints = Hints.empty

  val testBiggerUnion: FieldLens[PayloadData, Option[TestBiggerUnion]] = TestBiggerUnion.$schema.optional[PayloadData]("testBiggerUnion", _.testBiggerUnion, n => c => c.copy(testBiggerUnion = n))

  implicit val $schema: Schema[PayloadData] = struct(
    testBiggerUnion,
  ){
    PayloadData.apply
  }.withId($id).addHints($hints)
}
