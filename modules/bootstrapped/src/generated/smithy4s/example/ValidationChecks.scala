package smithy4s.example

import smithy.api.HttpQuery
import smithy.api.Length
import smithy.api.Range
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ValidationChecks(str: Option[String] = None, lst: Option[List[String]] = None, int: Option[Int] = None)
object ValidationChecks extends ShapeTag.Companion[ValidationChecks] {

  val str: FieldLens[ValidationChecks, Option[String]] = string.validated(Length(min = Some(1L), max = Some(10L))).optional[ValidationChecks]("str", _.str, n => c => c.copy(str = n)).addHints(HttpQuery("str"))
  val lst: FieldLens[ValidationChecks, Option[List[String]]] = StringList.underlyingSchema.validated(Length(min = Some(1L), max = Some(10L))).optional[ValidationChecks]("lst", _.lst, n => c => c.copy(lst = n)).addHints(HttpQuery("lst"))
  val int: FieldLens[ValidationChecks, Option[Int]] = smithy4s.schema.Schema.int.validated(Range(min = Some(scala.math.BigDecimal(1.0)), max = Some(scala.math.BigDecimal(10.0)))).optional[ValidationChecks]("int", _.int, n => c => c.copy(int = n)).addHints(HttpQuery("int"))

  implicit val schema: Schema[ValidationChecks] = struct(
    str,
    lst,
    int,
  ){
    ValidationChecks.apply
  }
  .withId(ShapeId("smithy4s.example", "ValidationChecks"))
}
