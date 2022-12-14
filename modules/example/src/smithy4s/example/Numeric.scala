package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.int
import smithy4s.Hints
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.double
import smithy4s.schema.Schema.bigint
import smithy4s.schema.Schema.bigdecimal
import smithy4s.schema.Schema.long
import smithy4s.ShapeId
import smithy4s.schema.Schema.float
import smithy4s.ShapeTag
import smithy4s.schema.Schema.short

case class Numeric(i: Option[Int] = None, f: Option[Float] = None, d: Option[Double] = None, s: Option[Short] = None, l: Option[Long] = None, bi: Option[BigInt] = None, bd: Option[BigDecimal] = None)
object Numeric extends ShapeTag.Companion[Numeric] {
  val id: ShapeId = ShapeId("smithy4s.example", "Numeric")

  val hints : Hints = Hints.empty

  implicit val schema: Schema[Numeric] = struct(
    int.optional[Numeric]("i", _.i),
    float.optional[Numeric]("f", _.f),
    double.optional[Numeric]("d", _.d),
    short.optional[Numeric]("s", _.s),
    long.optional[Numeric]("l", _.l),
    bigint.optional[Numeric]("bi", _.bi),
    bigdecimal.optional[Numeric]("bd", _.bd),
  ){
    Numeric.apply
  }.withId(id).addHints(hints)
}