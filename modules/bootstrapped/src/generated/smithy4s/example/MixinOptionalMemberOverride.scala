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

  val a = string.required[MixinOptionalMemberOverride]("a", _.a).addHints(smithy.api.Required())

  implicit val schema: Schema[MixinOptionalMemberOverride] = struct(
    a,
  ){
    MixinOptionalMemberOverride.apply
  }.withId(id).addHints(hints)
}
