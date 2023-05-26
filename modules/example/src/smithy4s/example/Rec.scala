package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Rec(name: String, next: Option[smithy4s.example.Rec] = None)
object Rec extends ShapeTag.Companion[Rec] {
  val id: ShapeId = ShapeId("smithy4s.example", "Rec")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Rec] = recursive(struct(
    string.required[Rec]("name", _.name).addHints(smithy.api.Required()),
    smithy4s.example.Rec.schema.optional[Rec]("next", _.next),
  ){
    Rec.apply
  }.withId(id).addHints(hints))
}
