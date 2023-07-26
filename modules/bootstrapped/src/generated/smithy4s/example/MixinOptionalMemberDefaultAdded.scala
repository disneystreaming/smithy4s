package smithy4s.example

import smithy.api.Default
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class MixinOptionalMemberDefaultAdded(a: String = "test")
object MixinOptionalMemberDefaultAdded extends ShapeTag.Companion[MixinOptionalMemberDefaultAdded] {

  val a = string.required[MixinOptionalMemberDefaultAdded]("a", _.a, n => c => c.copy(a = n)).addHints(Default(smithy4s.Document.fromString("test")))

  implicit val schema: Schema[MixinOptionalMemberDefaultAdded] = struct(
    a,
  ){
    MixinOptionalMemberDefaultAdded.apply
  }
  .withId(ShapeId("smithy4s.example", "MixinOptionalMemberDefaultAdded"))
  .addHints(
    Hints.empty
  )
}
