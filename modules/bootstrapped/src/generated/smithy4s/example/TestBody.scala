package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string

final case class TestBody(data: Option[String] = None)

object TestBody extends ShapeTag.Companion[TestBody] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestBody")

  val hints: Hints = Hints.empty

  object optics {
    val data: Lens[TestBody, Option[String]] = Lens[TestBody, Option[String]](_.data)(n => a => a.copy(data = n))
  }

  implicit val schema: Schema[TestBody] = struct(
    string.validated(smithy.api.Length(min = Some(10L), max = None)).optional[TestBody]("data", _.data),
  ){
    TestBody.apply
  }.withId(id).addHints(hints)
}
