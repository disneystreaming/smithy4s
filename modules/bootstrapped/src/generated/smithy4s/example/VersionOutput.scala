package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class VersionOutput(version: String)

object VersionOutput extends ShapeTag.Companion[VersionOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "VersionOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[VersionOutput] = struct(
    string.required[VersionOutput]("version", _.version).addHints(smithy.api.HttpPayload()),
  ){
    VersionOutput.apply
  }.withId(id).addHints(hints)
}
