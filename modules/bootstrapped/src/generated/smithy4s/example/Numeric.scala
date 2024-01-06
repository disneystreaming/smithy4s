package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.bigdecimal
import smithy4s.schema.Schema.bigint
import smithy4s.schema.Schema.double
import smithy4s.schema.Schema.float
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.short

final case class Numeric(i: Int = 1, f: Float = 1.0f, d: Double = 1.0d, s: Short = 1, l: Long = 1L, bi: BigInt = _root_.scala.math.BigInt(1), bd: BigDecimal = _root_.scala.math.BigDecimal(1.0))

object Numeric extends ShapeTag.Companion[Numeric] {
  val id: ShapeId = ShapeId("smithy4s.example", "Numeric")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Numeric] = struct(
    int.field[Numeric]("i", _.i).addHints(smithy.api.Default(_root_.smithy4s.Document.fromDouble(1.0d))),
    float.field[Numeric]("f", _.f).addHints(smithy.api.Default(_root_.smithy4s.Document.fromDouble(1.0d))),
    double.field[Numeric]("d", _.d).addHints(smithy.api.Default(_root_.smithy4s.Document.fromDouble(1.0d))),
    short.field[Numeric]("s", _.s).addHints(smithy.api.Default(_root_.smithy4s.Document.fromDouble(1.0d))),
    long.field[Numeric]("l", _.l).addHints(smithy.api.Default(_root_.smithy4s.Document.fromDouble(1.0d))),
    bigint.field[Numeric]("bi", _.bi).addHints(smithy.api.Default(_root_.smithy4s.Document.fromDouble(1.0d))),
    bigdecimal.field[Numeric]("bd", _.bd).addHints(smithy.api.Default(_root_.smithy4s.Document.fromDouble(1.0d))),
  ){
    Numeric.apply
  }.withId(id).addHints(hints)
}
