package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class StructWithOpenUnion(union: SampleOpenUnion, str: String)

object StructWithOpenUnion extends ShapeTag.Companion[StructWithOpenUnion] {
  val id: ShapeId = ShapeId("smithy4s.example", "StructWithOpenUnion")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(union: SampleOpenUnion, str: String): StructWithOpenUnion = StructWithOpenUnion(union, str)

  implicit val schema: Schema[StructWithOpenUnion] = struct(
    SampleOpenUnion.schema.required[StructWithOpenUnion]("union", _.union),
    string.required[StructWithOpenUnion]("str", _.str),
  )(make).withId(id).addHints(hints)
}
