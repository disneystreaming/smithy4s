package smithy4s.example

import smithy4s.Schema
import smithy4s.Hints
import smithy4s.schema.Schema.string
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag

case class MixinOptionalMemberDefaultAdded(a: String = "test")
object MixinOptionalMemberDefaultAdded extends ShapeTag.Companion[MixinOptionalMemberDefaultAdded] {
  val id: ShapeId = ShapeId("smithy4s.example", "MixinOptionalMemberDefaultAdded")

  val hints : Hints = Hints.empty

  implicit val schema: Schema[MixinOptionalMemberDefaultAdded] = struct(
    string.required[MixinOptionalMemberDefaultAdded]("a", _.a).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
  ){
    MixinOptionalMemberDefaultAdded.apply
  }.withId(id).addHints(hints)
}