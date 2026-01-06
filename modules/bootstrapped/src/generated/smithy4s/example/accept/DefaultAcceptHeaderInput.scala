package smithy4s.example.accept

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class DefaultAcceptHeaderInput(data: Option[String] = None)

object DefaultAcceptHeaderInput extends ShapeTag.Companion[DefaultAcceptHeaderInput] {
  val id: ShapeId = ShapeId("smithy4s.example.accept", "DefaultAcceptHeaderInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(data: Option[String]): DefaultAcceptHeaderInput = DefaultAcceptHeaderInput(data)

  implicit val schema: Schema[DefaultAcceptHeaderInput] = struct(
    string.optional[DefaultAcceptHeaderInput]("data", _.data),
  )(make).withId(id).addHints(hints)
}
