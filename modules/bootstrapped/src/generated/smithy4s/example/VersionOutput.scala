package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

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
