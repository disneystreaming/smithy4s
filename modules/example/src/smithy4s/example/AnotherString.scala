package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

/** Multiple line doc comment for another string
  * I'm putting a random \*\/ here. Who would do such a thing?
  * Because.
  * Seriously, it's import to escape special characters.
  */
object AnotherString extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "AnotherString")
  val hints: Hints = Hints(
    smithy.api.Documentation("Multiple line doc comment for another string\nI\'m putting a random */ here. Who would do such a thing?\nBecause.\nSeriously, it\'s import to escape special characters."),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[AnotherString] = bijection(underlyingSchema, asBijection)
}