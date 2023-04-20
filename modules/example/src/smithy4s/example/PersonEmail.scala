package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.example.typeclass.EqInterpreter
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object PersonEmail extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "PersonEmail")
  val hints: Hints = Hints(
    smithy4s.example.Eq(),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[PersonEmail] = bijection(underlyingSchema, asBijection)

  implicit val personEmailEq: cats.Eq[PersonEmail] = EqInterpreter.fromSchema(schema)
}