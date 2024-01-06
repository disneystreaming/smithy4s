package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class MixinOptionalMemberDefaultAdded(a: String = "test")

object MixinOptionalMemberDefaultAdded extends ShapeTag.Companion[MixinOptionalMemberDefaultAdded] {
  val id: ShapeId = ShapeId("smithy4s.example", "MixinOptionalMemberDefaultAdded")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[MixinOptionalMemberDefaultAdded] = struct(
    string.field[MixinOptionalMemberDefaultAdded]("a", _.a).addHints(smithy.api.Default(_root_.smithy4s.Document.fromString("test"))),
  ){
    MixinOptionalMemberDefaultAdded.apply
  }.withId(id).addHints(hints)
}
