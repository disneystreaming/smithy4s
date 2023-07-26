package smithy4s.example

import smithy.api.Range
import smithy.api.Required
import smithy.api.Suppress
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class RangeCheck(qty: Int)
object RangeCheck extends ShapeTag.Companion[RangeCheck] {

  val qty: FieldLens[RangeCheck, Int] = int.validated(Range(min = Some(scala.math.BigDecimal(1.0)), max = None)).required[RangeCheck]("qty", _.qty, n => c => c.copy(qty = n)).addHints(Required())

  implicit val schema: Schema[RangeCheck] = struct(
    qty,
  ){
    RangeCheck.apply
  }
  .withId(ShapeId("smithy4s.example", "RangeCheck"))
  .addHints(
    Suppress(List("UnreferencedShape")),
  )
}
