package smithy4s.example

import smithy.api.HttpPayload
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class VersionOutput(version: String)
object VersionOutput extends ShapeTag.Companion[VersionOutput] {

  val version = string.required[VersionOutput]("version", _.version, n => c => c.copy(version = n)).addHints(HttpPayload(), Required())

  implicit val schema: Schema[VersionOutput] = struct(
    version,
  ){
    VersionOutput.apply
  }
  .withId(ShapeId("smithy4s.example", "VersionOutput"))
  .addHints(
    Hints.empty
  )
}
