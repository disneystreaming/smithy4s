package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class ValidatedFoo(name: Option[ValidatedString] = None)

object ValidatedFoo extends ShapeTag.Companion[ValidatedFoo] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedFoo")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[ValidatedFoo] = struct(
    ValidatedString.schema.optional[ValidatedFoo]("name", _.name),
  ){
    ValidatedFoo.apply
  }.withId(id).addHints(hints)
}
