package smithy4s.example

import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class MixinOptionalMemberOverride(a: String)
object MixinOptionalMemberOverride extends ShapeTag.$Companion[MixinOptionalMemberOverride] {
  val $id: ShapeId = ShapeId("smithy4s.example", "MixinOptionalMemberOverride")

  val $hints: Hints = Hints.empty

  val a: FieldLens[MixinOptionalMemberOverride, String] = string.required[MixinOptionalMemberOverride]("a", _.a, n => c => c.copy(a = n)).addHints(Required())

  implicit val $schema: Schema[MixinOptionalMemberOverride] = struct(
    a,
  ){
    MixinOptionalMemberOverride.apply
  }.withId($id).addHints($hints)
}
