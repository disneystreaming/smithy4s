package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.struct

final case class PayloadData(testBiggerUnion: Option[TestBiggerUnion] = None)
object PayloadData extends ShapeTag.Companion[PayloadData] {
  val id: ShapeId = ShapeId("smithy4s.example", "PayloadData")

  val hints: Hints = Hints.empty

  object Lenses {
    val testBiggerUnion = Lens[PayloadData, Option[TestBiggerUnion]](_.testBiggerUnion)(n => a => a.copy(testBiggerUnion = n))
  }

  implicit val schema: Schema[PayloadData] = struct(
    TestBiggerUnion.schema.optional[PayloadData]("testBiggerUnion", _.testBiggerUnion),
  ){
    PayloadData.apply
  }.withId(id).addHints(hints)
}
