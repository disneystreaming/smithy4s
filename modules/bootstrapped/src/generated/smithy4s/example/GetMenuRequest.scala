package smithy4s.example

import smithy.api.HttpLabel
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetMenuRequest(restaurant: String)
object GetMenuRequest extends ShapeTag.Companion[GetMenuRequest] {

  val restaurant = string.required[GetMenuRequest]("restaurant", _.restaurant, n => c => c.copy(restaurant = n)).addHints(HttpLabel(), Required())

  implicit val schema: Schema[GetMenuRequest] = struct(
    restaurant,
  ){
    GetMenuRequest.apply
  }
  .withId(ShapeId("smithy4s.example", "GetMenuRequest"))
  .addHints(
    Hints.empty
  )
}
