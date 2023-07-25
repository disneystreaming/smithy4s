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

  val food = Food.schema.required[MenuItem]("food", _.food).addHints(smithy.api.Required())
  val price = float.required[MenuItem]("price", _.price).addHints(smithy.api.Required())

  implicit val schema: Schema[MenuItem] = struct(
    food,
    price,
  ){
    MenuItem.apply
  }.withId(id).addHints(hints)
}
