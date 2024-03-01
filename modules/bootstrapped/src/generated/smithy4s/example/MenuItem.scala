package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.float
import smithy4s.schema.Schema.struct

final case class MenuItem(food: Food, price: Float)

object MenuItem extends ShapeTag.Companion[MenuItem] {
  val id: ShapeId = ShapeId("smithy4s.example", "MenuItem")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(food: Food, price: Float): MenuItem = MenuItem(food, price)

  implicit val schema: Schema[MenuItem] = struct(
    Food.schema.required[MenuItem]("food", _.food),
    float.required[MenuItem]("price", _.price),
  ){
    make
  }.withId(id).addHints(hints)
}
