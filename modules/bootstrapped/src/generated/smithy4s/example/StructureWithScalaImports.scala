package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.refined.Age.provider._
import smithy4s.schema.Schema.struct


final case class StructureWithScalaImports(teenage: Option[Age] = None)

object StructureWithScalaImports extends ShapeTag.Companion[StructureWithScalaImports] {
  val id: ShapeId = ShapeId("smithy4s.example", "StructureWithScalaImports")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(teenage: Option[Age]): StructureWithScalaImports = StructureWithScalaImports(teenage)

  implicit val schema: Schema[StructureWithScalaImports] = struct(
    Age.schema.validated(smithy.api.Range(min = Some(scala.math.BigDecimal(13.0)), max = Some(scala.math.BigDecimal(19.0)))).optional[StructureWithScalaImports]("teenage", _.teenage),
  )(make).withId(id).addHints(hints)
}
