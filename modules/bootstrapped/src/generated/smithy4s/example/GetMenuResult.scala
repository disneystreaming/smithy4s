package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class GetMenuResult(menu: Map[String, MenuItem])

object GetMenuResult extends ShapeTag.Companion[GetMenuResult] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetMenuResult")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(menu: Map[String, MenuItem]): GetMenuResult = GetMenuResult(menu)

  implicit val schema: Schema[GetMenuResult] = struct(
    Menu.underlyingSchema.required[GetMenuResult]("menu", _.menu).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
