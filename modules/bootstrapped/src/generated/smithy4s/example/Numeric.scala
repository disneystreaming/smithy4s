package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bigdecimal
import smithy4s.schema.Schema.bigint
import smithy4s.schema.Schema.double
import smithy4s.schema.Schema.float
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.short
import smithy4s.schema.Schema.struct

final case class Numeric(i: Int = 1, f: Float = 1.0f, d: Double = 1.0d, s: Short = 1, l: Long = 1L, bi: BigInt = scala.math.BigInt(1), bd: BigDecimal = scala.math.BigDecimal(1.0))

object Numeric extends ShapeTag.Companion[Numeric] {
  val id: ShapeId = ShapeId("smithy4s.example", "Numeric")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(i: Int, f: Float, d: Double, s: Short, l: Long, bi: BigInt, bd: BigDecimal): Numeric = Numeric(i, f, d, s, l, bi, bd)

  implicit val schema: Schema[Numeric] = struct(
    int.field[Numeric]("i", _.i).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    float.field[Numeric]("f", _.f).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    double.field[Numeric]("d", _.d).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    short.field[Numeric]("s", _.s).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    long.field[Numeric]("l", _.l).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    bigint.field[Numeric]("bi", _.bi).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    bigdecimal.field[Numeric]("bd", _.bd).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
  )(make).withId(id).addHints(hints)
}
