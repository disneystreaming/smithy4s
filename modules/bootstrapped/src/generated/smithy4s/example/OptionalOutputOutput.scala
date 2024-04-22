package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class OptionalOutputOutput(body: Option[String] = None)

object OptionalOutputOutput extends ShapeTag.Companion[OptionalOutputOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "OptionalOutputOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(body: Option[String]): OptionalOutputOutput = OptionalOutputOutput(body)

  implicit val schema: Schema[OptionalOutputOutput] = struct(
    string.optional[OptionalOutputOutput]("body", _.body).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
