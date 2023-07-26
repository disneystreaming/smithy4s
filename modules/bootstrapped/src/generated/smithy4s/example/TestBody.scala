package smithy4s.example

import smithy.api.Length
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class TestBody(data: Option[String] = None)
object TestBody extends ShapeTag.Companion[TestBody] {

  val data = string.validated(Length(min = Some(10L), max = None)).optional[TestBody]("data", _.data, n => c => c.copy(data = n))

  implicit val schema: Schema[TestBody] = struct(
    data,
  ){
    TestBody.apply
  }
  .withId(ShapeId("smithy4s.example", "TestBody"))
  .addHints(
    Hints.empty
  )
}
