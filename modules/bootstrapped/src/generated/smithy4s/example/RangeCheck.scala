package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.int

final case class RangeCheck(qty: Int)

object RangeCheck extends ShapeTag.Companion[RangeCheck] {
  val id: ShapeId = ShapeId("smithy4s.example", "RangeCheck")

  val hints: Hints = Hints(
    smithy.api.Suppress(List("UnreferencedShape")),
  )

  implicit val schema: Schema[RangeCheck] = struct(
    int.validated(smithy.api.Range(min = Some(_root_.scala.math.BigDecimal(1.0)), max = None)).required[RangeCheck]("qty", _.qty),
  ){
    RangeCheck.apply
  }.withId(id).addHints(hints)
}
