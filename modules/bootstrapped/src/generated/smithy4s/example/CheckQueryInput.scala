package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class CheckQueryInput(inp: Map[String, List[String]] = Map())

object CheckQueryInput extends ShapeTag.Companion[CheckQueryInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "CheckQueryInput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(inp: Map[String, List[String]]): CheckQueryInput = CheckQueryInput(inp)

  implicit val schema: Schema[CheckQueryInput] = struct(
    QParams.underlyingSchema.field[CheckQueryInput]("inp", _.inp).addHints(smithy.api.Default(smithy4s.Document.obj()), smithy.api.HttpQueryParams()),
  )(make).withId(id).addHints(hints)
}
