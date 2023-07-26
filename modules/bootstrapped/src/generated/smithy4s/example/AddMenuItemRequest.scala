package smithy4s.example

import smithy.api.HttpLabel
import smithy.api.HttpPayload
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class AddMenuItemRequest(restaurant: String, menuItem: MenuItem)
object AddMenuItemRequest extends ShapeTag.$Companion[AddMenuItemRequest] {
  val $id: ShapeId = ShapeId("smithy4s.example", "AddMenuItemRequest")

  val $hints: Hints = Hints.empty

  val restaurant: FieldLens[AddMenuItemRequest, String] = string.required[AddMenuItemRequest]("restaurant", _.restaurant, n => c => c.copy(restaurant = n)).addHints(HttpLabel(), Required())
  val menuItem: FieldLens[AddMenuItemRequest, MenuItem] = MenuItem.$schema.required[AddMenuItemRequest]("menuItem", _.menuItem, n => c => c.copy(menuItem = n)).addHints(HttpPayload(), Required())

  implicit val $schema: Schema[AddMenuItemRequest] = struct(
    restaurant,
    menuItem,
  ){
    AddMenuItemRequest.apply
  }.withId($id).addHints($hints)
}
