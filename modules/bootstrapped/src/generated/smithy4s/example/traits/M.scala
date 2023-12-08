package smithy4s.example.traits

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object M extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example.traits", "M")
  val hints: Hints = Hints(
    smithy4s.example.traits.RecursiveViaTraitMember(),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[M] = bijection(underlyingSchema, asBijection)
}
