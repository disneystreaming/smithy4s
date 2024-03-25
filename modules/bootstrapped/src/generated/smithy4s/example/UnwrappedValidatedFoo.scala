package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class UnwrappedValidatedFoo(name: Option[String] = None)

object UnwrappedValidatedFoo extends ShapeTag.Companion[UnwrappedValidatedFoo] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnwrappedValidatedFoo")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[UnwrappedValidatedFoo] = struct(
    UnwrappedValidatedString.underlyingSchema.optional[UnwrappedValidatedFoo]("name", _.name),
  ){
    UnwrappedValidatedFoo.apply
  }.withId(id).addHints(hints)
}
