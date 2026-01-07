package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class StructForDiscrimination(str: String)

object StructForDiscrimination extends ShapeTag.Companion[StructForDiscrimination] {
  val id: ShapeId = ShapeId("smithy4s.example", "StructForDiscrimination")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(str: String): StructForDiscrimination = StructForDiscrimination(str)

  implicit val schema: Schema[StructForDiscrimination] = struct(
    string.required[StructForDiscrimination]("str", _.str),
  )(make).withId(id).addHints(hints)
}
