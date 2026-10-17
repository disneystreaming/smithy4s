package smithy4s.example.content

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class DefaultContentHeaderOutput(result: Option[String] = None)

object DefaultContentHeaderOutput extends ShapeTag.Companion[DefaultContentHeaderOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.content", "DefaultContentHeaderOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(result: Option[String]): DefaultContentHeaderOutput = DefaultContentHeaderOutput(result)

  implicit val schema: Schema[DefaultContentHeaderOutput] = struct(
    string.optional[DefaultContentHeaderOutput]("result", _.result).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
