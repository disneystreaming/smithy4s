package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class AddMenuItemRequest(restaurant: String, menuItem: MenuItem)

object AddMenuItemRequest extends ShapeTag.Companion[AddMenuItemRequest] {
  val id: ShapeId = ShapeId("smithy4s.example", "AddMenuItemRequest")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(restaurant: String, menuItem: MenuItem): AddMenuItemRequest = AddMenuItemRequest(restaurant, menuItem)

  implicit val schema: Schema[AddMenuItemRequest] = struct(
    string.required[AddMenuItemRequest]("restaurant", _.restaurant).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
    MenuItem.schema.required[AddMenuItemRequest]("menuItem", _.menuItem).addHints(Hints.dynamic(ShapeId("smithy.api", "httpPayload"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
