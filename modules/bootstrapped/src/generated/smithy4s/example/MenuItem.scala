package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.float

final case class MenuItem(food: Food, price: Float)

object MenuItem extends ShapeTag.Companion[MenuItem] {
  val id: ShapeId = ShapeId("smithy4s.example", "MenuItem")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[MenuItem] = struct(
    Food.schema.required[MenuItem]("food", _.food),
    float.required[MenuItem]("price", _.price),
  ){
    MenuItem.apply
  }.withId(id).addHints(hints)
}
