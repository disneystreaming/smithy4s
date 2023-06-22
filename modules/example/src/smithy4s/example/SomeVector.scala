package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.vector

object SomeVector extends Newtype[Vector[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "SomeVector")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Vector[String]] = vector(string).withId(id).addHints(hints)
  implicit val schema: Schema[SomeVector] = bijection(underlyingSchema, asBijection)
}