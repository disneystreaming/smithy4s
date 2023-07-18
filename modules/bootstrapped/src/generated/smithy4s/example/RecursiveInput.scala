package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class RecursiveInput(hello: Option[smithy4s.example.RecursiveInput] = None)
object RecursiveInput extends ShapeTag.Companion[RecursiveInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "RecursiveInput")

  val hints: Hints = Hints.empty

  object Lenses {
    val hello = Lens[RecursiveInput, Option[smithy4s.example.RecursiveInput]](_.hello)(n => a => a.copy(hello = n))
  }

  implicit val schema: Schema[RecursiveInput] = recursive(struct(
    smithy4s.example.RecursiveInput.schema.optional[RecursiveInput]("hello", _.hello),
  ){
    RecursiveInput.apply
  }.withId(id).addHints(hints))
}
