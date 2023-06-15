package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Salad(name: String, ingredients: List[Ingredient])
object Salad extends ShapeTag.Companion[Salad] {
  val id: ShapeId = ShapeId("smithy4s.example", "Salad")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Salad] = struct(
    string.required[Salad]("name", _.name).addHints(smithy.api.Required()),
    Ingredients.underlyingSchema.required[Salad]("ingredients", _.ingredients).addHints(smithy.api.Required()),
  ){
    Salad.apply
  }.withId(id).addHints(hints)
}
