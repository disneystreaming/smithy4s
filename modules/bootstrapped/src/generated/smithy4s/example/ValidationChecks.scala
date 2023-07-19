package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ValidationChecks(str: Option[String] = None, lst: Option[List[String]] = None, int: Option[Int] = None)
object ValidationChecks extends ShapeTag.Companion[ValidationChecks] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidationChecks")

  val hints: Hints = Hints.empty

  object Optics {
    val str = Lens[ValidationChecks, Option[String]](_.str)(n => a => a.copy(str = n))
    val lst = Lens[ValidationChecks, Option[List[String]]](_.lst)(n => a => a.copy(lst = n))
    val int = Lens[ValidationChecks, Option[Int]](_.int)(n => a => a.copy(int = n))
  }

  implicit val schema: Schema[ValidationChecks] = struct(
    string.validated(smithy.api.Length(min = Some(1L), max = Some(10L))).optional[ValidationChecks]("str", _.str).addHints(smithy.api.HttpQuery("str")),
    StringList.underlyingSchema.validated(smithy.api.Length(min = Some(1L), max = Some(10L))).optional[ValidationChecks]("lst", _.lst).addHints(smithy.api.HttpQuery("lst")),
    int.validated(smithy.api.Range(min = Some(scala.math.BigDecimal(1.0)), max = Some(scala.math.BigDecimal(10.0)))).optional[ValidationChecks]("int", _.int).addHints(smithy.api.HttpQuery("int")),
  ){
    ValidationChecks.apply
  }.withId(id).addHints(hints)
}
