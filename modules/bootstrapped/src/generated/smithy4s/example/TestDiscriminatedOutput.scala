package smithy4s.example

import smithy.api.HttpPayload
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class TestDiscriminatedOutput(data: Option[PayloadData] = None)
object TestDiscriminatedOutput extends ShapeTag.Companion[TestDiscriminatedOutput] {

  val data = PayloadData.schema.optional[TestDiscriminatedOutput]("data", _.data, n => c => c.copy(data = n)).addHints(HttpPayload())

  implicit val schema: Schema[TestDiscriminatedOutput] = struct(
    data,
  ){
    TestDiscriminatedOutput.apply
  }
  .withId(ShapeId("smithy4s.example", "TestDiscriminatedOutput"))
  .addHints(
    Hints.empty
  )
}
