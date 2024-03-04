package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class PackedInput(key: String)

object PackedInput extends ShapeTag.Companion[PackedInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "PackedInput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(key: String): PackedInput = PackedInput(key)

  implicit val schema: Schema[PackedInput] = struct(
    string.required[PackedInput]("key", _.key),
  )(make).withId(id).addHints(hints)
}
