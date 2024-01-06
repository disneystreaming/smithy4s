package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string

final case class ValidationChecks(str: Option[String] = None, lst: Option[List[String]] = None, int: Option[Int] = None)

object ValidationChecks extends ShapeTag.Companion[ValidationChecks] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidationChecks")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[ValidationChecks] = struct(
    string.validated(smithy.api.Length(min = Some(1L), max = Some(10L))).optional[ValidationChecks]("str", _.str).addHints(smithy.api.HttpQuery("str")),
    StringList.underlyingSchema.validated(smithy.api.Length(min = Some(1L), max = Some(10L))).optional[ValidationChecks]("lst", _.lst).addHints(smithy.api.HttpQuery("lst")),
    int.validated(smithy.api.Range(min = Some(_root_.scala.math.BigDecimal(1.0)), max = Some(_root_.scala.math.BigDecimal(10.0)))).optional[ValidationChecks]("int", _.int).addHints(smithy.api.HttpQuery("int")),
  ){
    ValidationChecks.apply
  }.withId(id).addHints(hints)
}
