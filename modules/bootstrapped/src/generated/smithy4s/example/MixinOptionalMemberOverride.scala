package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class MixinOptionalMemberOverride(a: String)

object MixinOptionalMemberOverride extends ShapeTag.Companion[MixinOptionalMemberOverride] {
  val id: ShapeId = ShapeId("smithy4s.example", "MixinOptionalMemberOverride")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(a: String): MixinOptionalMemberOverride = MixinOptionalMemberOverride(a)

  implicit val schema: Schema[MixinOptionalMemberOverride] = struct(
    string.required[MixinOptionalMemberOverride]("a", _.a),
  )(make).withId(id).addHints(hints)
}
