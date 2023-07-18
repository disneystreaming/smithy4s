package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.struct

final case class TestDiscriminatedOutput(data: Option[PayloadData] = None)
object TestDiscriminatedOutput extends ShapeTag.Companion[TestDiscriminatedOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestDiscriminatedOutput")

  val hints: Hints = Hints.empty

  object Lenses {
    val data = Lens[TestDiscriminatedOutput, Option[PayloadData]](_.data)(n => a => a.copy(data = n))
  }

  implicit val schema: Schema[TestDiscriminatedOutput] = struct(
    PayloadData.schema.optional[TestDiscriminatedOutput]("data", _.data).addHints(smithy.api.HttpPayload()),
  ){
    TestDiscriminatedOutput.apply
  }.withId(id).addHints(hints)
}
