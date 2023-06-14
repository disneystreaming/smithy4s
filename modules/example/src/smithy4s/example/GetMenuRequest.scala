package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetMenuRequest(restaurant: String)
object GetMenuRequest extends ShapeTag.Companion[GetMenuRequest] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetMenuRequest")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[GetMenuRequest] = struct(
    string.required[GetMenuRequest]("restaurant", _.restaurant).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
  ){
    GetMenuRequest.apply
  }.withId(id).addHints(hints)
}
