package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class TestBody(data: Option[String] = None)
object TestBody extends ShapeTag.Companion[TestBody] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestBody")

  val hints: Hints = Hints.empty

  object Optics {
    val data: Lens[TestBody, Option[String]] = Lens[TestBody, Option[String]](_.data)(n => a => a.copy(data = n))
  }

  implicit val schema: Schema[TestBody] = struct(
    string.validated(smithy.api.Length(min = Some(10L), max = None)).optional[TestBody]("data", _.data),
  ){
    TestBody.apply
  }.withId(id).addHints(hints)
}
