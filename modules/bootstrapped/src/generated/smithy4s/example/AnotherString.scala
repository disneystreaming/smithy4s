package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

/** Multiple line doc comment for another string
  * Containing a random \*\/ here.
  * Seriously, it's important to escape special characters.
  */
object AnotherString extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "AnotherString")
  val hints: Hints = Hints(
    ShapeId("smithy.api", "documentation") -> Document.fromString("Multiple line doc comment for another string\nContaining a random */ here.\nSeriously, it\'s important to escape special characters."),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[AnotherString] = bijection(underlyingSchema, asBijection)
}
