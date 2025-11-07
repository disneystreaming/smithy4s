package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class CheckQueryInput(inp: Option[Map[String, List[String]]] = None)

object CheckQueryInput extends ShapeTag.Companion[CheckQueryInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "CheckQueryInput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(inp: Option[Map[String, List[String]]]): CheckQueryInput = CheckQueryInput(inp)

  implicit val schema: Schema[CheckQueryInput] = struct(
    QParams.underlyingSchema.optional[CheckQueryInput]("inp", _.inp).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc), Hints.dynamic(ShapeId("smithy.api", "httpQueryParams"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
