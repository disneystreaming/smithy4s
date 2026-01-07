package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class RefinedIntWrapped(int: Int)

object RefinedIntWrapped extends ShapeTag.Companion[RefinedIntWrapped] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "RefinedIntWrapped")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(int: Int): RefinedIntWrapped = RefinedIntWrapped(int)

  implicit val schema: Schema[RefinedIntWrapped] = struct(
    int.validated(smithy.api.Range(min = Some(scala.math.BigDecimal(1.0)), max = Some(scala.math.BigDecimal(10.0)))).required[RefinedIntWrapped]("int", _.int),
  )(make).withId(id).addHints(hints)
}
