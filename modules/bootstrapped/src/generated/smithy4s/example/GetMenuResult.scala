package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct

final case class GetMenuResult(menu: Map[String, MenuItem])

object GetMenuResult extends ShapeTag.Companion[GetMenuResult] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetMenuResult")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[GetMenuResult] = struct(
    Menu.underlyingSchema.required[GetMenuResult]("menu", _.menu).addHints(smithy.api.HttpPayload()),
  ){
    GetMenuResult.apply
  }.withId(id).addHints(hints)
}
