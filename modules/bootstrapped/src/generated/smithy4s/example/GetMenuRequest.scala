package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class GetMenuRequest(restaurant: String)

object GetMenuRequest extends ShapeTag.Companion[GetMenuRequest] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetMenuRequest")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[GetMenuRequest] = struct(
    string.required[GetMenuRequest]("restaurant", _.restaurant).addHints(smithy.api.HttpLabel()),
  ){
    GetMenuRequest.apply
  }.withId(id).addHints(hints)
}
