package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class MixinOptionalMemberOverride(a: String)

object MixinOptionalMemberOverride extends ShapeTag.Companion[MixinOptionalMemberOverride] {
  val id: ShapeId = ShapeId("smithy4s.example", "MixinOptionalMemberOverride")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[MixinOptionalMemberOverride] = struct(
    string.required[MixinOptionalMemberOverride]("a", _.a),
  ){
    MixinOptionalMemberOverride.apply
  }.withId(id).addHints(hints)
}
