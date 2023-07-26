package smithy4s.example

import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.float
import smithy4s.schema.Schema.struct

final case class MenuItem(food: Food, price: Float)
object MenuItem extends ShapeTag.$Companion[MenuItem] {
  val $id: ShapeId = ShapeId("smithy4s.example", "MenuItem")

  val $hints: Hints = Hints.empty

  val food: FieldLens[MenuItem, Food] = Food.$schema.required[MenuItem]("food", _.food, n => c => c.copy(food = n)).addHints(Required())
  val price: FieldLens[MenuItem, Float] = float.required[MenuItem]("price", _.price, n => c => c.copy(price = n)).addHints(Required())

  implicit val $schema: Schema[MenuItem] = struct(
    food,
    price,
  ){
    MenuItem.apply
  }.withId($id).addHints($hints)
}
