package smithy4s.example.content

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class DefaultContentHeaderInput(data: String)

object DefaultContentHeaderInput extends ShapeTag.Companion[DefaultContentHeaderInput] {
  val id: ShapeId = ShapeId("smithy4s.example.content", "DefaultContentHeaderInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(data: String): DefaultContentHeaderInput = DefaultContentHeaderInput(data)

  implicit val schema: Schema[DefaultContentHeaderInput] = struct(
    string.required[DefaultContentHeaderInput]("data", _.data).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
