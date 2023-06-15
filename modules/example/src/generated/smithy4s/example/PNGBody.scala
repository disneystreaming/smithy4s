package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class PNGBody(png: PNG)
object PNGBody extends ShapeTag.Companion[PNGBody] {
  val id: ShapeId = ShapeId("smithy4s.example", "PNGBody")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[PNGBody] = struct(
    PNG.schema.required[PNGBody]("png", _.png).addHints(smithy.api.HttpPayload(), smithy.api.Required()),
  ){
    PNGBody.apply
  }.withId(id).addHints(hints)
}
