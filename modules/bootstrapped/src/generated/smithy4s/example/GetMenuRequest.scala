package smithy4s.example

import smithy.api.HttpLabel
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetMenuRequest(restaurant: String)
object GetMenuRequest extends ShapeTag.$Companion[GetMenuRequest] {
  val $id: ShapeId = ShapeId("smithy4s.example", "GetMenuRequest")

  val $hints: Hints = Hints.empty

  val restaurant: FieldLens[GetMenuRequest, String] = string.required[GetMenuRequest]("restaurant", _.restaurant, n => c => c.copy(restaurant = n)).addHints(HttpLabel(), Required())

  implicit val $schema: Schema[GetMenuRequest] = struct(
    restaurant,
  ){
    GetMenuRequest.apply
  }.withId($id).addHints($hints)
}