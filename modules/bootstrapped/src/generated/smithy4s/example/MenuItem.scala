package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.float
import smithy4s.schema.Schema.struct

final case class MenuItem(food: Food, price: Float, tags: Option[List[String]] = None, extraData: Option[Map[String, String]] = None)

object MenuItem extends ShapeTag.Companion[MenuItem] {
  val id: ShapeId = ShapeId("smithy4s.example", "MenuItem")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(food: Food, price: Float, tags: Option[List[String]], extraData: Option[Map[String, String]]): MenuItem = MenuItem(food, price, tags, extraData)

  implicit val schema: Schema[MenuItem] = struct(
    Food.schema.required[MenuItem]("food", _.food),
    float.required[MenuItem]("price", _.price),
    Tags.underlyingSchema.optional[MenuItem]("tags", _.tags),
    ExtraData.underlyingSchema.optional[MenuItem]("extraData", _.extraData),
  )(make).withId(id).addHints(hints)
}
