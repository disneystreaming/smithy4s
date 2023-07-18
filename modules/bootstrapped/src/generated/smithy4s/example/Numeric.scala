package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
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

  object Lenses {
    val i = Lens[Numeric, Int](_.i)(n => a => a.copy(i = n))
    val f = Lens[Numeric, Float](_.f)(n => a => a.copy(f = n))
    val d = Lens[Numeric, Double](_.d)(n => a => a.copy(d = n))
    val s = Lens[Numeric, Short](_.s)(n => a => a.copy(s = n))
    val l = Lens[Numeric, Long](_.l)(n => a => a.copy(l = n))
    val bi = Lens[Numeric, BigInt](_.bi)(n => a => a.copy(bi = n))
    val bd = Lens[Numeric, BigDecimal](_.bd)(n => a => a.copy(bd = n))
  }

  implicit val schema: Schema[Numeric] = struct(
    int.required[Numeric]("i", _.i).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    float.required[Numeric]("f", _.f).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    double.required[Numeric]("d", _.d).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    short.required[Numeric]("s", _.s).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    long.required[Numeric]("l", _.l).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    bigint.required[Numeric]("bi", _.bi).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    bigdecimal.required[Numeric]("bd", _.bd).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
  ){
    Numeric.apply
  }.withId(id).addHints(hints)
}
