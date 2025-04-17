package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class MixinRequiredMemberDefaultAdded(description: String = "different description") extends MixinRequiredMember

object MixinRequiredMemberDefaultAdded extends ShapeTag.Companion[MixinRequiredMemberDefaultAdded] {
  val id: ShapeId = ShapeId("smithy4s.example", "MixinRequiredMemberDefaultAdded")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(description: String): MixinRequiredMemberDefaultAdded = MixinRequiredMemberDefaultAdded(description)

  implicit val schema: Schema[MixinRequiredMemberDefaultAdded] = struct(
    string.required[MixinRequiredMemberDefaultAdded]("description", _.description).addHints(smithy.api.Default(smithy4s.Document.fromString("different description"))),
  )(make).withId(id).addHints(hints)
}
