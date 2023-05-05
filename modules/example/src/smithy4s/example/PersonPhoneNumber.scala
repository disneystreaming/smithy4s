package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.example.typeclass.EqInterpreter
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object PersonPhoneNumber extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "PersonPhoneNumber")
  val hints: Hints = Hints(
    smithy4s.example.Eq(),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[PersonPhoneNumber] = bijection(underlyingSchema, asBijection)

  implicit val personPhoneNumberEq: cats.Eq[PersonPhoneNumber] = EqInterpreter.fromSchema(schema)
}
