package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GreedyAbcDefInput(abc: String)

object GreedyAbcDefInput extends ShapeTag.Companion[GreedyAbcDefInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GreedyAbcDefInput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(abc: String): GreedyAbcDefInput = GreedyAbcDefInput(abc)

  implicit val schema: Schema[GreedyAbcDefInput] = struct(
    string.required[GreedyAbcDefInput]("abc", _.abc).addHints(smithy.api.HttpLabel()),
  )(make).withId(id).addHints(hints)
}
