package smithy4s.example

import smithy.api.Default
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.bigdecimal
import smithy4s.schema.Schema.bigint
import smithy4s.schema.Schema.double
import smithy4s.schema.Schema.float
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.short
import smithy4s.schema.Schema.struct

final case class Numeric(i: Int = 1, f: Float = 1.0f, d: Double = 1.0d, s: Short = 1, l: Long = 1L, bi: BigInt = scala.math.BigInt(1), bd: BigDecimal = scala.math.BigDecimal(1.0))
object Numeric extends ShapeTag.$Companion[Numeric] {
  val $id: ShapeId = ShapeId("smithy4s.example", "Numeric")

  val $hints: Hints = Hints.empty

  val i: FieldLens[Numeric, Int] = int.required[Numeric]("i", _.i, n => c => c.copy(i = n)).addHints(Default(smithy4s.Document.fromDouble(1.0d)))
  val f: FieldLens[Numeric, Float] = float.required[Numeric]("f", _.f, n => c => c.copy(f = n)).addHints(Default(smithy4s.Document.fromDouble(1.0d)))
  val d: FieldLens[Numeric, Double] = double.required[Numeric]("d", _.d, n => c => c.copy(d = n)).addHints(Default(smithy4s.Document.fromDouble(1.0d)))
  val s: FieldLens[Numeric, Short] = short.required[Numeric]("s", _.s, n => c => c.copy(s = n)).addHints(Default(smithy4s.Document.fromDouble(1.0d)))
  val l: FieldLens[Numeric, Long] = long.required[Numeric]("l", _.l, n => c => c.copy(l = n)).addHints(Default(smithy4s.Document.fromDouble(1.0d)))
  val bi: FieldLens[Numeric, BigInt] = bigint.required[Numeric]("bi", _.bi, n => c => c.copy(bi = n)).addHints(Default(smithy4s.Document.fromDouble(1.0d)))
  val bd: FieldLens[Numeric, BigDecimal] = bigdecimal.required[Numeric]("bd", _.bd, n => c => c.copy(bd = n)).addHints(Default(smithy4s.Document.fromDouble(1.0d)))

  implicit val $schema: Schema[Numeric] = struct(
    i,
    f,
    d,
    s,
    l,
    bi,
    bd,
  ){
    Numeric.apply
  }.withId($id).addHints($hints)
}
