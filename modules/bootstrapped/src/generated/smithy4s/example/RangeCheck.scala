package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class RangeCheck(qty: Int)
object RangeCheck extends ShapeTag.Companion[RangeCheck] {
  val id: ShapeId = ShapeId("smithy4s.example", "RangeCheck")

  val hints: Hints = Hints(
    smithy.api.Suppress(List("UnreferencedShape")),
  )

  implicit val schema: Schema[RangeCheck] = struct(
    int.validated(smithy.api.Range(min = Some(scala.math.BigDecimal(1.0)), max = None)).required[RangeCheck]("qty", _.qty),
  ){
    RangeCheck.apply
  }.withId(id).addHints(hints)
}
