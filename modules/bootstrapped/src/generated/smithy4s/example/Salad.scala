package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class Salad(name: String, ingredients: List[Ingredient])

object Salad extends ShapeTag.Companion[Salad] {
  val id: ShapeId = ShapeId("smithy4s.example", "Salad")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Salad] = struct(
    string.required[Salad]("name", _.name),
    Ingredients.underlyingSchema.required[Salad]("ingredients", _.ingredients),
  ){
    Salad.apply
  }.withId(id).addHints(hints)
}
