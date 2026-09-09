package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bigdecimal
import smithy4s.schema.Schema.bigint
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.struct

final case class BigNumeric(bi: BigInt = scala.math.BigInt("4294967296"), bd: BigDecimal = scala.math.BigDecimal("9007199254740993"), doc: Document = smithy4s.Document.fromBigDecimal(scala.math.BigDecimal("18446744073709551616")), l: Option[Long] = None)

object BigNumeric extends ShapeTag.Companion[BigNumeric] {
  val id: ShapeId = ShapeId("smithy4s.example", "BigNumeric")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(bi: BigInt, bd: BigDecimal, l: Option[Long], doc: Document): BigNumeric = BigNumeric(bi, bd, doc, l)

  implicit val schema: Schema[BigNumeric] = struct[BigNumeric](
    bigint.field[BigNumeric]("bi", _.bi).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromLong(4294967296L))),
    bigdecimal.field[BigNumeric]("bd", _.bd).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromLong(9007199254740993L))),
    long.validated(smithy.api.Range(min = Some(scala.math.BigDecimal("-9007199254740993")), max = Some(scala.math.BigDecimal("9007199254740993")))).optional[BigNumeric]("l", _.l),
    document.field[BigNumeric]("doc", _.doc).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromBigDecimal(scala.math.BigDecimal("18446744073709551616")))),
  )(make).withId(id).addHints(hints)
}
