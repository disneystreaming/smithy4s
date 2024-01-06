package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct

final case class TestDiscriminatedOutput(data: Option[PayloadData] = None)

object TestDiscriminatedOutput extends ShapeTag.Companion[TestDiscriminatedOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestDiscriminatedOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[TestDiscriminatedOutput] = struct(
    PayloadData.schema.optional[TestDiscriminatedOutput]("data", _.data).addHints(smithy.api.HttpPayload()),
  ){
    TestDiscriminatedOutput.apply
  }.withId(id).addHints(hints)
}
