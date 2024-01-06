package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class AddMenuItemRequest(restaurant: String, menuItem: MenuItem)

object AddMenuItemRequest extends ShapeTag.Companion[AddMenuItemRequest] {
  val id: ShapeId = ShapeId("smithy4s.example", "AddMenuItemRequest")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[AddMenuItemRequest] = struct(
    string.required[AddMenuItemRequest]("restaurant", _.restaurant).addHints(smithy.api.HttpLabel()),
    MenuItem.schema.required[AddMenuItemRequest]("menuItem", _.menuItem).addHints(smithy.api.HttpPayload()),
  ){
    AddMenuItemRequest.apply
  }.withId(id).addHints(hints)
}
